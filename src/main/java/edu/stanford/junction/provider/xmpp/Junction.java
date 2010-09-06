package edu.stanford.junction.provider.xmpp;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.api.messaging.target.MessageTarget;
import edu.stanford.junction.provider.ExtrasDirector;

public class Junction extends edu.stanford.junction.Junction {
	
	//TODO: XMPP won't let you query for room information
	// if the room is private.
	// Update getActivityScript() to join the room and get info.
	// or break the spec...
	private boolean PUBLIC_ROOM = true;
	
	public static String NS_JX = "jx";
	private ActivityScript mActivityDescription;
	private JunctionActor mOwner;
	private JunctionProvider mProvider;
	
	protected XMPPConnection mXMPPConnection;
	private MultiUserChat mSessionChat;
	private ConnectionListener mDebugConnectionListener;
	PacketFilter mMessageFilter = null;
	
	protected URI mAcceptedInvitation = null;
	/**
	 * Creates a new activity and registers it
	 * with a Junction server.
	 * 
	 * TODO: probably merge this function with registerActor().
	 */
	protected Junction(ActivityScript desc, XMPPConnection xmppConnection, 
					   XMPPSwitchboardConfig xmppConfig, JunctionProvider prov) {
		
		PacketFilter typeFilter = new OrFilter(new MessageTypeFilter(Message.Type.chat), 
											   new MessageTypeFilter(Message.Type.groupchat));

		PacketFilter addrFilter = new FromContainsFilter("@"+xmppConfig.getChatService());
		
		mMessageFilter = new AndFilter(typeFilter,addrFilter);
		
		if (xmppConnection == null) {
			throw new IllegalArgumentException("XMPPConnection must not be null.");
		}
		mActivityDescription=desc;
		mXMPPConnection = xmppConnection;
		mDebugConnectionListener = new ConnectionListener();
		mXMPPConnection.addConnectionListener(mDebugConnectionListener);
		mProvider=prov;
	}
	
	public String getActivityID() {
		return mActivityDescription.getActivityID();
	}
	
	public ActivityScript getActivityScript() {
		return mActivityDescription;
	}
	
	
	public void registerActor(final JunctionActor actor) {
		//System.out.print("adding actor for roles: ");
		//String[] roles =  actor.getRoles();
		/*for(int i = 0; i<roles.length; i++) 
		  System.out.print(roles[i] + " ");
		  System.out.print("\n");*/
		
		mOwner = actor;
		mOwner.setJunction(this);
		
		List<JunctionExtra> extras = actor.getInitialExtras();
		if (extras != null){
			for (int i=0;i<extras.size();i++) {
				registerExtra(extras.get(i));
			}
		}
		
		try {
			mSessionChat = joinSessionChat();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		MessageHandler handler = new MessageHandler() {
				@Override
				public void onMessageReceived(MessageHeader header,
											  JSONObject message) {
				
					Junction.this.triggerMessageReceived(header, message);
				}
			};
		
		if (handler != null) {
			registerMessageHandler(handler);
		}
		
		Junction.this.triggerActorJoin(mActivityDescription.isActivityCreator());
	}
	
	public void start() {
		Map<String,String>go = new HashMap<String,String>();
		
		try {
			mSessionChat.sendMessage(go.toString());
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		mSessionChat.leave();
		mProvider.remove(this);
	}
	
	public String[] getRoles() {
		return mActivityDescription.getRoles();
	}

	public String getSessionID() {
		return mActivityDescription.getSessionID();
	}
	public String getSwitchboard() {
		return mActivityDescription.getHost();
	}

	public void registerMessageHandler(final MessageHandler handler) {
		PacketListener packetListener = new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					Message message = (Message)packet;
					//System.out.println("got message " + message.toXML());
				
					JSONObject obj = null;
					try {
						obj = new JSONObject(message.getBody());
					} catch (Exception e) {
						System.out.println("Could not convert to json: " + message.getBody());
						//e.printStackTrace();
						return;
					}
				
					if (obj.has(NS_JX)) {
						JSONObject header = obj.optJSONObject(NS_JX);
						if (header.has("targetRole")) {
							String target = header.optString("targetRole");
							String[] roles = mOwner.getRoles();
							boolean forMe=false;
							for (int i=0;i<roles.length;i++) {
								if (roles[i].equals(target)) {
									forMe=true;
									break;
								}
								if (!forMe) return;
							}
						}
					}
					int i;
					String from = message.getFrom();
					if ((i =from.lastIndexOf('/')) >= 0) {
						from = from.substring(i+1);
					}
					handler.onMessageReceived(new MessageHeader(Junction.this,obj,from), obj);
				}
			};
		
		mXMPPConnection.addPacketListener(packetListener, mMessageFilter);
		//mSessionChat.addMessageListener(packetListener);
	}

	
	public void sendMessageToTarget(MessageTarget target, JSONObject message) {
		target.sendMessage(message);
	}
	
	public void doSendMessageToActor(String actorID, JSONObject message) {
		try {
			String privChat = mSessionChat.getRoom()+"/" + actorID;
			Chat chat = mSessionChat.createPrivateChat(privChat,null);
			chat.sendMessage(message.toString());
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	public void doSendMessageToRole(String role, JSONObject message) {
		try {
			JSONObject jx;
			if (message.has(NS_JX)) {
				jx = message.optJSONObject(NS_JX);
			} else {
				jx = new JSONObject();
				try {
					message.put(NS_JX, jx);
				} catch (JSONException j) {}
			}
			try {
				jx.put("targetRole", role);
			} catch (Exception e) {}
			mSessionChat.sendMessage(message.toString());
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

	public void doSendMessageToSession(JSONObject message) {
		try {
			mSessionChat.sendMessage(message.toString());
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

	public URI getBaseInvitationURI() {
		URI invitation = null;
		try {
			// TODO: strip query part from hostURL
			invitation = new URI("junction://"
								 +getSwitchboard()+"/"
								 +getSessionID()
								 );
		} catch (Exception e) {
			e.printStackTrace();
		}

		return invitation;
	}
	
	
	private MultiUserChat joinSessionChat() throws XMPPException {
		String room = mActivityDescription.getSessionID()+"@conference." + mXMPPConnection.getServiceName();
		
		DiscussionHistory history = new DiscussionHistory();
		history.setMaxChars(0);
		MultiUserChat chat = new MultiUserChat(mXMPPConnection, room);

		System.out.println("Joining " + room);
		//if (mActivityDescription.isActivityOwner()) {
		try {
			try {
				MultiUserChat.getRoomInfo(mXMPPConnection, room);
				chat.join(mOwner.getActorID(),null,history,10000);
				mActivityDescription.isActivityCreator(false);
				return chat;
			} catch (Exception e) { /*e.printStackTrace();*/ }
				
			System.out.println("Trying to create room");
				
				
			// TODO: is this an error? is there really a notion of ownership?
			mActivityDescription.isActivityCreator(true);
			chat.create(mOwner.getActorID());

			//mSessionChat.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
				
			System.out.println("sending config form");
			Form form = chat.getConfigurationForm();
			// Create a new form to submit based on the original form
			Form submitForm = form.createAnswerForm();
			// Add default answers to the form to submit
			for (Iterator<FormField> fields = form.getFields(); fields.hasNext();) {
				FormField field = (FormField) fields.next();
				//System.out.println(field.getVariable());
				if ("muc#roomconfig_roomdesc".equals(field.getVariable())) {
					//System.out.println("setting the room desc " + mActivityDescription.getJSON().toString());
					submitForm.setAnswer("muc#roomconfig_roomdesc", mActivityDescription.getJSON().toString());
				} else if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
					// Sets the default value as the answer
					submitForm.setDefaultAnswer(field.getVariable());
				}
			}
			      
			List<String>whois = new ArrayList<String>();
			whois.add("moderators");
			submitForm.setAnswer("muc#roomconfig_whois", whois);
			submitForm.setAnswer("muc#roomconfig_publicroom", PUBLIC_ROOM);
			chat.sendConfigurationForm(submitForm);
				
				
		} catch (XMPPException e) {
			System.out.println("Could not create room");
			e.printStackTrace();
			try {
				chat.join(mOwner.getActorID(),null,history,10000);
			} catch (XMPPException e2) {
				System.err.println("could not join or create room. ");
				e2.printStackTrace();
			}
		}
		return chat;
	}

	@Override
	public URI getAcceptedInvitation() {
		return mAcceptedInvitation;
	}

	@Override
	public JunctionActor getActor() {
		return mOwner;
	}

	class ConnectionListener implements org.jivesoftware.smack.ConnectionListener{

		/**
		 * Notification that the connection was closed normally or that the reconnection
		 * process has been aborted.
		 */
		public void connectionClosed(){
			System.out.println("XMPPConnection closed. ");
		}

		/**
		 * Notification that the connection was closed due to an exception. When
		 * abruptly disconnected it is possible for the connection to try reconnecting
		 * to the server.
		 *
		 * @param e the exception.
		 */
		public void connectionClosedOnError(Exception e){
			System.out.println("XMPPConnection closed due to error. Attempting reconnect. ");
		}
    
		/**
		 * The connection will retry to reconnect in the specified number of seconds.
		 * 
		 * @param seconds remaining seconds before attempting a reconnection.
		 */
		public void reconnectingIn(int seconds){
			System.out.println("XMPPConnection attempting reconnect in " + seconds + " seconds.");
		}
    
		/**
		 * The connection has reconnected successfully to the server. Connections will
		 * reconnect to the server when the previous socket connection was abruptly closed.
		 */
		public void reconnectionSuccessful(){
			System.out.println("XMPPConnection reconnected successfully!");
		}
    
		/**
		 * An attempt to connect to the server has failed. The connection will keep trying
		 * reconnecting to the server in a moment.
		 *
		 * @param e the exception that caused the reconnection to fail.
		 */
		public void reconnectionFailed(Exception e){
			System.out.println("XMPPConnection reconnect failed!");
		}
	}
}