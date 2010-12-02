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
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class JXServer {
	public static final int SERVER_PORT = 8283;
	private static final String TAG = "jx_server";
	private static final int READ_BUFFER = 2048;
	
	private Map<String,JSONObject> mActivityScripts;
	private Set<ConnectedThread> mConnections;
	private Map<RoomId, Map<String, ConnectedThread>> mSubscriptions;
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
					sendMessageToActor(this.getActorID(), new JSONObject("{\"msg\":\"hello world!!\"}"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
		
		URI uri = URI.create("junction://localhost/hoodat#jx");
		SwitchboardConfig cfg = JunctionMaker.getDefaultSwitchboardConfig(uri); 
		ActivityScript script = new ActivityScript();
		script.setFriendlyName("Test Session");
		script.setActivityID("org.openjunction.test");
		
		
		try {
			Log.d(TAG, "Attempting to join session");
			JunctionMaker.getInstance(cfg).newJunction(uri, script, actor);
		} catch (JunctionException e) {
			Log.e(TAG, "error joining juction", e);
		}
	}
	
	
	public JXServer() {
		
	}
	
	/**
	 * Starts a simple chat server, allowing users to
	 * connect to an arbitrary chat room.
	 */
	public void start() {
		mConnections = new HashSet<ConnectedThread>();
		mSubscriptions = new HashMap<RoomId, Map<String, ConnectedThread>>();
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
        private Set<RoomOccupancy> mmSubscriptions;

        public ConnectedThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSubscriptions = new HashSet<RoomOccupancy>();
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

            // Read header information, determine connection type
            try {
            	bytes = mmInStream.read(buffer);
            	String header = new String(buffer, 0, bytes);
            	
            	// determine request type
            	if (header.startsWith("GET ")) {
            		Log.d(TAG, "Found HTTP GET request");
            		doHttpConnection();
            	} else if (header.startsWith("JUNCTION")) {
            		Log.d(TAG, "Found Junction connection");
            		doJunctionConnection();
            	}
            } catch (IOException e) {
            	Log.e(TAG, "Error reading connection header", e);
            }
            
            // No longer listening.
            cancel();
        }
        
        private void doHttpConnection() {
        	String response = "<html><em>Coming soon: The Long Poll.</em></html>";
    		write(response.getBytes(), response.length());
        }
        
        private void doJunctionConnection() {
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
	                    handleJson(json);
                    } catch (JSONException e) {
                    	// Log.e(TAG, "JSON error", e);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    break;
                }
            }
        }
        
        private void handleJson(JSONObject json) {
        	try {
	        	if (json.has(Junction.NS_JX)) {
	            	JSONObject jx = json.getJSONObject(Junction.NS_JX);
	            	if (jx.has(Junction.JX_SYS_MSG)) {
	                	JSONObject sys = jx.getJSONObject(Junction.JX_SYS_MSG);
	                	// Join
	                	if (sys.has("join")) {
	                		synchronized (JXServer.this) {
		                		String room = sys.getString("join");
		                		String me = sys.getString("id");
		                		mmSubscriptions.add(new RoomOccupancy(getRoomId(room), me));
		                		
		                		Map<String, ConnectedThread> participants;
		                		if (mSubscriptions.containsKey(room)) {
		                			participants = mSubscriptions.get(room);
		                			JSONObject script = mActivityScripts.get(room);
		                			if (script != null) {
		                				JSONObject aScriptObj = new JSONObject();
		        	                    JSONObject aScriptMsg = new JSONObject();
		        	                    try {
		        		                    aScriptObj.put(Junction.JX_SYS_MSG, true);
		        		                    aScriptObj.put(Junction.JX_SCRIPT, script);
		        		                    aScriptMsg.put(Junction.NS_JX, aScriptObj);
		        	                    } catch (JSONException e) {}
		        	                    
		        	                    byte[] bts = aScriptMsg.toString().getBytes();
		        	                    this.write(bts, bts.length);
		                			}
		                			
		                		} else {
		                			JSONObject script = sys.optJSONObject("script");
		                			participants = new HashMap<String, ConnectedThread>();
		                			mSubscriptions.put(getRoomId(room), participants);
		                			mActivityScripts.put(room, script);
		                		}
		                		participants.put(me, this);
	                		}
	                	}
	                	
	                	// Send message to session
	                	String action = sys.optString("action");
	                	if ("send_s".equals(action)) {
	                		String session = sys.getString("session");
	                		RoomId room = getRoomId(session);
	                		jx.remove(Junction.JX_SYS_MSG);
	                		
	                		byte[] outbytes = json.toString().getBytes();
	                		synchronized(room) {
	                			Map<String, ConnectedThread> peers = mSubscriptions.get(room);
	                			for (String u : peers.keySet()) {
	                				ConnectedThread conn = peers.get(u);
	                				conn.write(outbytes, outbytes.length);
	                			}
	                		}
	                	}
	                	
	                	if ("send_a".equals(action)) {
	                		String session = sys.getString("session");
	                		RoomId room = getRoomId(session);
	                		String actor = sys.getString("actor");
	                		jx.remove(Junction.JX_SYS_MSG);
	                		
	                		byte[] outbytes = json.toString().getBytes();
	                		synchronized(room) {
	                			ConnectedThread conn = mSubscriptions.get(room).get(actor);
	                			if (conn != null) {
	                				conn.write(outbytes, outbytes.length);
	                			}
	                		}
	                	}
	            	}
	            }
        	} catch (JSONException e) {
        		Log.e(TAG, "Error building json object", e);
        	}
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
	            	for (RoomOccupancy entry : mmSubscriptions) {
	            		synchronized(entry.room) {
		            		Map<String, ConnectedThread> users = mSubscriptions.get(entry.room);
		            		users.remove(entry.id);
		            		if (users.size() == 0) {
		            			mSubscriptions.remove(entry.room);
		            		}
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
	
    public class RoomOccupancy {
    	public RoomId room;
    	public String id;
    	
    	public RoomOccupancy(RoomId r, String me) {
    		room = r;
    		id = me;
    	}
    }
    
    /**
     * Use a wrapper class so we can better trust locks
     */
    public class RoomId {
    	public String name;
    	
    	private RoomId(String name) {
    		this.name = name;
    	}
    }
    
    Map<String,RoomId> mRoomMap = new HashMap<String,RoomId>();
    public RoomId getRoomId(String name) {
    	if (!mRoomMap.containsKey(name)) {
    		mRoomMap.put(name, new RoomId(name));
    	}
    	return mRoomMap.get(name);
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
