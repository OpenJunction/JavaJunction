package edu.stanford.junction.provider.jx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class JXServer {
	public static final int SERVER_PORT = 8283;
	private static final String TAG = "jx_server";
	private static final int READ_BUFFER = 2048;
	
	private Map<String,JSONObject> mActivityScripts;
	private Set<ConnectedThread> mConnections;
	private Map<String,Set<ConnectedThread>> mSubscriptions;
	private AcceptThread mAcceptThread;
	
	
	public static void main(String[] argv) {
		final String TAG = "test";
		
		JXServer server = new JXServer();
		Log.d(TAG, "Starting server.");
		server.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		JunctionActor actor = new JunctionActor("tester") {
			
			@Override
			public void onMessageReceived(MessageHeader header, JSONObject message) {
				Log.d(TAG, "got: " + message.toString() + " !");
			}
			
			@Override
			public void onActivityJoin() {
				super.onActivityJoin();
				Log.d(TAG, "joined session!");
				try {
					sendMessageToSession(new JSONObject("{\"msg\":\"hello world!!\"}"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
		
		JXSwitchboardConfig cfg = new JXSwitchboardConfig();
		ActivityScript script = null;
		URI uri = URI.create("junction://localhost/hoodat#jx");
		
		try {
			Log.d(TAG, "Attempting to join session");
			JunctionMaker.getInstance(cfg).newJunction(uri, script, actor);
		} catch (JunctionException e) {
			Log.e(TAG, "error joining juction", e);
		}
	}
	
	
	private JXServer() {
		
	}
	
	public void start() {
		mConnections = new HashSet<ConnectedThread>();
		mSubscriptions = new HashMap<String, Set<ConnectedThread>>();
		mActivityScripts = new HashMap<String, JSONObject>();
		mAcceptThread = new AcceptThread();
		mAcceptThread.start();
	}
	
	public void stop() {
		mConnections.clear();
		mConnections = null;
		
		mSubscriptions.clear();
		mSubscriptions = null;
		
		mActivityScripts.clear();
		mActivityScripts = null;
		
		mAcceptThread.cancel();
		mAcceptThread = null;
	}
	
	private class AcceptThread extends Thread {
        // The local server socket
        private final ServerSocket mmServerSocket;

        public AcceptThread() {
            ServerSocket tmp = null;
            
            // Create a new listening server socket
            try {
                tmp = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                System.err.println("Could not open server socket");
                e.printStackTrace(System.err);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            //Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            Socket socket = null;

            // Listen to the server socket always
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	Log.d(TAG, "waiting for client...");
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "Client connected!");
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket == null) {
                	break;
                }
                
                synchronized (JXServer.this) {
                    ConnectedThread conThread = new ConnectedThread(socket);
                    conThread.start();
                    mConnections.add(conThread);
                }
            }
            Log.d(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }

            for (ConnectedThread conn : mConnections) {
        		conn.cancel();
            }
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
        private Set<String> mmSubscriptions;

        public ConnectedThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSubscriptions = new HashSet<String>();
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
	                    Log.d(TAG, "read: " + jsonStr);
	                    JSONObject json = new JSONObject(jsonStr);
	                    
	                    if (json.has(Junction.NS_JX)) {
	                    	JSONObject jx = json.getJSONObject(Junction.NS_JX);
	                    	if (jx.has(Junction.JX_SYS_MSG)) {
	                        	JSONObject sys = jx.getJSONObject(Junction.JX_SYS_MSG);
	                        	Log.d(TAG, "HAS SYS PIECE MORE");
	                        	// Join
	                        	if (sys.has("join")) {
	                        		String room = sys.getString("join");
	                        		JSONObject script = sys.optJSONObject("script");
	                        		mmSubscriptions.add(room);
	                        		
	                        		Set<ConnectedThread> participants;
	                        		if (mSubscriptions.containsKey(room)) {
	                        			participants = mSubscriptions.get(room);
	                        		} else {
	                        			participants = new HashSet<ConnectedThread>();
	                        			mSubscriptions.put(room, participants);
	                        			mActivityScripts.put(room, script);
	                        		}
	                        		participants.add(this);
	                        	}
	                        	
	                        	// Send message to session
	                        	if (sys.has("send_s")) {
	                        		String session = sys.getString("send_s");
	                        		jx.remove(Junction.JX_SYS_MSG);
	                        		
	                        		byte[] outbytes = json.toString().getBytes();
	                        		synchronized(JXServer.this) {
	                        			for (ConnectedThread conn : mSubscriptions.get(session)) {
	                        				conn.write(outbytes, outbytes.length);
	                        			}
	                        		}
	                        	}
	                    	}
	                    }
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
            	synchronized(JXServer.this) {
	            	mConnections.remove(this);
	            	for (String channel : mmSubscriptions) {
	            		Set<ConnectedThread> sub = mSubscriptions.get(channel);
	            		sub.remove(this);
	            		if (sub.size() == 0) {
	            			mSubscriptions.remove(channel);
	            		}
	            	}
	            	mmSubscriptions.clear();
	            	mmSubscriptions = null;
            	}
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
	
    
    
    
    
	public static class Log {
		public static void d(String tag, String msg) {
			System.out.println(tag + ": " + msg);
		}
		
		public static void e(String tag, String msg) {
			System.err.println(tag + ": " + msg);
		}
		
		public static void e(String tag, String msg, Exception e) {
			System.err.println(tag + ": " + msg);
			e.printStackTrace(System.err);
		}
	}
}
