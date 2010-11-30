/*
 * Copyright (C) 2010 Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package edu.stanford.junction.provider.jx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.jx.JXServer.Log;

public class Junction extends edu.stanford.junction.Junction {
	private static String TAG = "jx_client";
	public static String JX_SYS_MSG = "jxsysmsg";
	public static String JX_NS = "jx";
	
	private final URI mAcceptedInvitation;
	private final String mSession;
	private ActivityScript mActivityScript;
	private static final int READ_BUFFER = 2048;
	
	private ConnectedThread mConnectedThread;
	
	public Junction(URI uri, ActivityScript script, final JunctionActor actor) {
		this.setActor(actor);
		
		mAcceptedInvitation = uri;
		mActivityScript = script;
		mSession = uri.getPath().substring(1);
		String host = uri.getHost();
		int port = uri.getPort();
		if (port == -1) port = JXServer.SERVER_PORT;
		
		// TODO: one connection per host (multiple subscriptions through one socket)
		// handle in Provider
		try {
			Socket socket = new Socket(host, port);
			mConnectedThread = new ConnectedThread(socket);
			mConnectedThread.start();
		} catch (IOException e) {
			Log.e(TAG, "Error connecting to socket", e);
		}
		
		triggerActorJoin(script == null || script.isActivityCreator());
	}
	
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public URI getAcceptedInvitation() {
		return mAcceptedInvitation;
	}

	@Override
	public ActivityScript getActivityScript() {
		return mActivityScript;
	}

	@Override
	public URI getBaseInvitationURI() {
		try {
			return new URI("junction://localhost#jx");
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String getSessionID() {
		return mSession;
	}

	@Override
	public String getSwitchboard() {
		return mAcceptedInvitation.getHost();
	}

	@Override
	public void doSendMessageToActor(String actorID, JSONObject message) {
		
	}

	@Override
	public void doSendMessageToRole(String role, JSONObject message) {
		
	}

	@Override
	public void doSendMessageToSession(JSONObject message) {
		try {
			JSONObject jx;
			if (message.has(NS_JX)) {
				jx = message.getJSONObject(NS_JX);
			} else {
				jx = new JSONObject();
				message.put(NS_JX, jx);
			}
			
			JSONObject send = new JSONObject();
			send.put("send_s", mSession);
			jx.put(JX_SYS_MSG, send);
			byte[] bytes = message.toString().getBytes();
			synchronized(Junction.this) {
				mConnectedThread.write(bytes, bytes.length);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Failed to send message", e);
		}
	}
	
	
	
	
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
            connect();
        }
        
        public void connect() {
        	JSONObject join = new JSONObject();
        	try {
        		JSONObject greeting = new JSONObject();
        		greeting.put("join", mSession);
        		greeting.put("actorId", getActor().getActorID());
        		if (mActivityScript != null) {
        			greeting.put("script", mActivityScript.getJSON());
        		}
        		
        		JSONObject envelop = new JSONObject();
        		envelop.put(JX_SYS_MSG, greeting);
        		join.put(NS_JX, envelop);
        	} catch (JSONException e) {
				throw new AssertionError("Bad JSON");
			}
        	
        	byte[] out = join.toString().getBytes();
        	write(out,out.length);
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[READ_BUFFER];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    if (bytes < 0) {
                    	break;
                    }

                    // TODO: won't work with something larger than READ_BUFFER.
                    try {
	                    String jsonStr = new String(buffer,0,bytes);
	                    JSONObject json = new JSONObject(jsonStr);
	                    
	                    if (json.has(NS_JX)) {
	                    	JSONObject sys = json.getJSONObject(NS_JX);
	                    	if (sys.has(JX_SYS_MSG)) {
	                        	JSONObject msg = json.getJSONObject(NS_JX);
	                        	return;
	                    	}
	                    }
	                    
	                    String from = "me";
	                    MessageHeader header = new MessageHeader(Junction.this, json, from);
	                    triggerMessageReceived(header, json);
                    } catch (JSONException e) {
                    	// Log.e(TAG, "JSON error", e);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    break;
                }
            }
            
            // No longer listening.
            cancel();
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer, int bytes) {
            try {
                mmOutStream.write(buffer, 0, bytes);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
	
}