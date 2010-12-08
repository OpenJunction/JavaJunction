package edu.stanford.junction.provider.jx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
	private static final int BUFFER_LENGTH = 1024;
	
	private Map<RoomId, JSONObject> mActivityScripts;
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
		
		final JunctionActor a1 = server.new TestActor("a1");
		final JunctionActor a2 = server.new TestActor("a2");
		final JunctionActor a3 = server.new TestActor("a3");
		
		
		final URI uri = URI.create("junction://localhost/hoodat#jx");
		final SwitchboardConfig cfg = JunctionMaker.getDefaultSwitchboardConfig(uri); 
		final ActivityScript script = new ActivityScript();
		script.setFriendlyName("Test Session");
		script.setActivityID("org.openjunction.test");
		
		boolean TEST_CLIENTS = true;
		if (TEST_CLIENTS) {
			try {
				Log.d(TAG, "Attempting to join session");
				JunctionMaker.getInstance(cfg).newJunction(uri, script, a1);
				JunctionMaker.getInstance(cfg).newJunction(uri, script, a2);
				JunctionMaker.getInstance(cfg).newJunction(uri, script, a3);
			} catch (JunctionException e) {
				Log.e(TAG, "error joining juction", e);
			}
		}
	}
	
	
	class TestActor extends JunctionActor {
		final String name;
		public TestActor(String name) {
			super("test");
			this.name = name;
		}
		
		@Override
		public void onMessageReceived(MessageHeader header, JSONObject message) {
			Log.d(TAG, name + " got: " + message.toString() + " !" + " from " + header.from);
		}
		
		@Override
		public void onActivityJoin() {
			super.onActivityJoin();
			Log.d(TAG, name + " joined session!");
			try {
				sendMessageToSession(new JSONObject("{\"msg\":\"hello world!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! from: " + name + "\"}"));
				Log.d(TAG, name + " sent a message.");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onActivityCreate() {
			Log.d(TAG, "CREATED the session!!!");
		}
	};
	
	
	
	public JXServer() {
		
	}
	
	/**
	 * Starts a simple chat server, allowing users to
	 * connect to an arbitrary chat room.
	 */
	public void start() {
		mConnections = new HashSet<ConnectedThread>();
		mSubscriptions = new ConcurrentHashMap<RoomId, Map<String, ConnectedThread>>();
		mActivityScripts = new ConcurrentHashMap<RoomId, JSONObject>();
		mAcceptThread = new AcceptThread();
		mAcceptThread.start();
	}
	
	public void stop() {
		mAcceptThread.cancel();
		mAcceptThread = null;
		mConnections = null;
		
		mSubscriptions.clear();
		mSubscriptions = null;
		
		mActivityScripts.clear();
		mActivityScripts = null;
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
                } catch (SocketException e) {
                	
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket == null) {
                	break;
                }
                
                //synchronized (JXServer.this) {
                    ConnectedThread conThread = new ConnectedThread(socket);
                    conThread.start();
                    mConnections.add(conThread);
                //}
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
            mConnections.clear();
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
        private final JsonHelper mmJsonHelper;

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
            mmJsonHelper = new JsonHelper(mmInStream, mmOutStream);
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[BUFFER_LENGTH];
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
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    JSONObject json = mmJsonHelper.jsonFromStream();
                    if (json == null) {
                    	break;
                    }
                    handleJson(json);
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
	                		boolean isCreator = false;
	                		
	                		String roomName = sys.getString("join");
	                		RoomId joinRoom = getRoomId(roomName);
	                		String me = sys.getString("id");
	                		synchronized (joinRoom) {
		                		//Log.d(TAG, "Adding " + me.substring(0,6) + " to " + room);
		                		mmSubscriptions.add(new RoomOccupancy(joinRoom, me));
		                		
		                		Map<String, ConnectedThread> participants;
		                		if (mSubscriptions.containsKey(joinRoom)) {
		                			// Joining existing session
		                			participants = mSubscriptions.get(joinRoom);
		                			isCreator = false;
		                		} else {
		                			// New session
		                			isCreator = true;
		                			participants = new HashMap<String, ConnectedThread>();
		                			mSubscriptions.put(joinRoom, participants);
		                			
		                			JSONObject script = sys.optJSONObject("script");
		                			if (script != null) {
			                			mActivityScripts.put(joinRoom, script);
		                			}
		                		}
		                		
		                		participants.put(me, this);
		                		
		                		// Response
		                		JSONObject script = null;
                				JSONObject joinedObj = new JSONObject();
        	                    JSONObject joinedMsg = new JSONObject();
        	                    try {
        		                    joinedObj.put(Junction.JX_SYS_MSG, true);
        		                    joinedObj.put(Junction.JX_JOINED, true);
        		                    joinedObj.put(Junction.JX_CREATOR, isCreator);
        		                    if (isCreator) {
        		                    	script = mActivityScripts.get(joinRoom);
        		                    	if (script != null) {
        		                    		joinedObj.put(Junction.JX_SCRIPT, script);
        		                    	}
        		                    }
        		                    joinedMsg.put(Junction.NS_JX, joinedObj);
        		                    mmJsonHelper.sendJson(joinedMsg);
        	                    } catch (Exception e) {
        	                    	Log.e(TAG, "Error sending join response",e);
        	                    }
	                		}
	                	}
	                	
	                	// Send message to session
	                	String action = sys.optString("action");
	                	if ("send_s".equals(action)) {
	                		String session = sys.getString("session");
	                		RoomId room = getRoomId(session);
	                		jx.remove(Junction.JX_SYS_MSG);
	                		
	                		synchronized(room) {
	                			Map<String, ConnectedThread> peers = mSubscriptions.get(room);
	                			for (String u : peers.keySet()) {
	                				ConnectedThread conn = peers.get(u);
	                				conn.sendJson(json);
	                			}
	                		}
	                	}
	                	
	                	if ("send_a".equals(action)) {
	                		String session = sys.getString("session");
	                		RoomId room = getRoomId(session);
	                		String actor = sys.getString("actor");
	                		jx.remove(Junction.JX_SYS_MSG);
	                		
	                		synchronized(room) {
	                			ConnectedThread conn = mSubscriptions.get(room).get(actor);
	                			if (conn != null) {
	                				conn.sendJson(json);
	                			}
	                		}
	                	}
	            	}
	        	}
        	} catch (JSONException e) {
        		Log.e(TAG, "Error building json object", e);
        	}
        }

        public void sendJson(JSONObject json) {
        	try {
        		mmJsonHelper.sendJson(json);
        	} catch (Exception e) {
        		Log.e(TAG, "Error writing JSON", e);
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
            	//synchronized(JXServer.this) {
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
            	//}
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
