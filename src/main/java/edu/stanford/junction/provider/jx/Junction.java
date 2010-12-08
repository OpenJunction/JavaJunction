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

import edu.stanford.junction.JunctionException;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.jx.JXServer.Log;

public class Junction extends edu.stanford.junction.Junction {
	private static String TAG = "jx_client";
	public static String JX_SYS_MSG = "jxsysmsg";
	public static String JX_SCRIPT = "ascript";
	public static String JX_JOINED = "joined";
	public static String JX_CREATOR = "creator";
	public static String JX_NS = "jx";
	
	private final URI mAcceptedInvitation;
	private final String mSession;
	private ActivityScript mActivityScript;
	private Object mJoinLock = new Object();
	private boolean mJoinComplete = false;
	private boolean mActivityCreator = false;
	
	private ConnectedThread mConnectedThread;
	private JXServer mSwitchboardServer = null;
	
	public Junction(URI uri, ActivityScript script, final JunctionActor actor) throws JunctionException {
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
			try {
				String my_ip = JunctionProvider.getLocalIpAddress();
				if (my_ip.equals(host)) {
					Log.d(TAG, "Starting local switchboard service");
					mSwitchboardServer = new JXServer();
					mSwitchboardServer.start();
				}
			} catch (Exception e) {
				Log.e(TAG, "Could not start local switchboard service", e);
			}
			
			Socket socket = new Socket(host, port);
			mConnectedThread = new ConnectedThread(socket);
			mConnectedThread.start();
		} catch (IOException e) {
			Log.e(TAG, "Error connecting to socket", e);
		}
		
		int MAX_TIME = 20000;
		if (!mJoinComplete) {
			try {
				synchronized(mJoinLock) {
					mJoinLock.wait(MAX_TIME);
				}
			} catch (InterruptedException e) {
				// Ignored
			}
		}
		if (!mJoinComplete) {
			throw new JunctionException("Timeout while joining Junction session.");
		}
		
		triggerActorJoin(mActivityCreator);
	}
	
	@Override
	public void disconnect() {
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
		}
		
		if (mSwitchboardServer != null) {
			mSwitchboardServer.stop();
		}
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
		try {
			JSONObject jx;
			if (message.has(NS_JX)) {
				jx = message.getJSONObject(NS_JX);
			} else {
				jx = new JSONObject();
				message.put(NS_JX, jx);
			}
			
			JSONObject send = new JSONObject();
			send.put("action", "send_a");
			send.put("session", mSession);
			send.put("actor", actorID);
			jx.put(JX_SYS_MSG, send);
			mConnectedThread.sendJson(message);
		} catch (Exception e) {
			Log.e(TAG, "Failed to send message", e);
		}
	}

	@Override
	public void doSendMessageToRole(String role, JSONObject message) {
		// TODO
		doSendMessageToSession(message);
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
			send.put("action", "send_s");
			send.put("session", mSession);
			jx.put(JX_SYS_MSG, send);
			mConnectedThread.sendJson(message);
		} catch (Exception e) {
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
        private final JsonHelper mJsonHelper;

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
            mJsonHelper = new JsonHelper(mmInStream, mmOutStream);
            
            connect();
        }
        
        public void connect() {
        	JSONObject join = new JSONObject();
        	
        	// Header info
        	try {
	        	byte[] header = "JUNCTION".getBytes();
	        	mmOutStream.write(header, 0, header.length);
	        	mmOutStream.flush();
	        	
	        	// TODO: fix socket timing issues and remove me
	        	try {
	        		Thread.sleep(800);
	        	}catch (Exception e) {}
        	} catch (IOException e) {
        		Log.e(TAG, "Error writing connection header");
        		return;
        	}
        	
        	// Join request
        	try {
        		JSONObject greeting = new JSONObject();
        		greeting.put("join", mSession);
        		greeting.put("id", getActor().getActorID());
        		if (mActivityScript != null) {
        			greeting.put("script", mActivityScript.getJSON());
        		}
        		
        		JSONObject envelop = new JSONObject();
        		envelop.put(JX_SYS_MSG, greeting);
        		join.put(NS_JX, envelop);
        	} catch (JSONException e) {
				throw new AssertionError("Bad JSON");
			}
        	
        	try {
        		mJsonHelper.sendJson(join);
        	} catch (IOException e) {
        		Log.e(TAG, "Error writing activity script", e);
        	}
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    JSONObject json = mJsonHelper.jsonFromStream();
                    if (json == null) {
                    	break;
                    }

                    if (json.has(NS_JX)) {
                    	JSONObject sys = json.getJSONObject(NS_JX);
                    	if (sys.has(JX_SYS_MSG)) {
                    		if (sys.has(JX_JOINED)) {
                    			if (sys.has(JX_SCRIPT)) {
                    				mActivityScript = new ActivityScript(sys.getJSONObject(JX_SCRIPT));
                    			}
                    			if (sys.has(JX_CREATOR) && sys.getBoolean(JX_CREATOR)) {
                    				mActivityCreator = true;
                    			} else {
                    				mActivityCreator = false;
                    			}
                    		}
                    		mJoinComplete = true;
                    		synchronized (mJoinLock) {
                    			mJoinLock.notify();
                    		}
                    	}
                    }
                    
                    String from = "me";
                    MessageHeader header = new MessageHeader(Junction.this, json, from);
                    triggerMessageReceived(header, json);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    break;
                } catch (JSONException e) {
                     	Log.e(TAG, "JSON error", e);
                }
            }
            
            // No longer listening.
            cancel();
        }

        public void sendJson(JSONObject json) {
        	try {
        		mJsonHelper.sendJson(json);
        	} catch (IOException e) {
        		Log.e(TAG, "Error sending json", e);
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