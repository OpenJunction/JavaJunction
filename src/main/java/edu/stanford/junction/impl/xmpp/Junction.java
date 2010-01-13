package edu.stanford.junction.impl.xmpp;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
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
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.api.messaging.target.MessageTarget;

public class Junction extends edu.stanford.junction.Junction {
	public static String NS_JX = "jx";
	private ActivityScript mActivityDescription;
	private JunctionActor mOwner;
	
	private String mXMPPServer;
	private XMPPConnection mXMPPConnection;
	private MultiUserChat mSessionChat;
	
	/**
	 * Creates a new activity and registers it
	 * with a Junction server.
	 * 
	 * TODO: probably merge this function with registerActor().
	 */
	protected Junction(ActivityScript desc, XMPPConnection xmppConnection) {

		mActivityDescription=desc;
		mXMPPConnection = xmppConnection;
		mXMPPServer=mActivityDescription.getHost();
		
	}
	
	public String getActivityID() {
		return mActivityDescription.getActivityID();
	}
	
	public ActivityScript getActivityDescription() {
		return mActivityDescription;
	}
	
	
	public void registerActor(final JunctionActor actor) {
		System.out.print("adding actor for roles: ");
		String[] roles =  actor.getRoles();
		for(int i = 0; i<roles.length; i++) 
			System.out.print(roles[i] + " ");
		System.out.print("\n");
		
		mOwner = actor;
		mOwner.setJunction(this);
		
		try {
			mSessionChat = joinSessionChat();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MessageHandler handler = new MessageHandler() {
			@Override
			public void onMessageReceived(MessageHeader header,
					JSONObject message) {
				
				actor.onMessageReceived(header, message);
			}
		};
		
		if (handler != null) {
			registerMessageHandler(handler);
		}
		if (mActivityDescription.isActivityCreator()) {
			mOwner.onActivityCreate();
		}
		
		mOwner.onActivityJoin();
		
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
		if (mXMPPConnection != null) {
			mXMPPConnection.disconnect();
			mXMPPConnection = null;
		}
	}
	
	
	class OnStartListener extends MessageHandler {
		private boolean started=false;
		public void onMessageReceived(MessageHeader header, JSONObject message) {
			
			/*for (JunctionActor actor : mActors) {
					actor.onActivityStart();
			}*/
			mOwner.onActivityStart();
			started=true;
			// mManager.removeListener(this);
		}
	}



	public List<String> getActorsForRole(String role) {
		// inefficient but who cares. small map.
		List<String>results = new ArrayList<String>();
		
		return results;
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
		
		mSessionChat.addMessageListener(packetListener);
	}

	
	public void sendMessageToTarget(MessageTarget target, JSONObject message) {
		target.sendMessage(message);
	}
	
	public void sendMessageToActor(String actorID, JSONObject message) {
	
		try {
			Chat chat = mSessionChat.createPrivateChat(mSessionChat.getRoom()+"/"+actorID,
					null);
		
			chat.sendMessage(message.toString());
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessageToRole(String role, JSONObject message) {
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

	public void sendMessageToSession(JSONObject message) {
		try {
			mSessionChat.sendMessage(message.toString());
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
	}

	public URI getInvitationURI() {
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

	public URI getInvitationURI(String requestedRole) {
		URI invitation = null;
		try {
			// TODO: strip query part from hostURL
			invitation = new URI("junction://"
								+getSwitchboard()+"/"
								+getSessionID()
								+"?role="+requestedRole
								);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return invitation;
	}
	
	
	private MultiUserChat joinSessionChat() throws XMPPException {
		String room = mActivityDescription.getSessionID()+"@conference."+mXMPPServer;
		
		DiscussionHistory history = new DiscussionHistory();
		history.setMaxChars(0);
		MultiUserChat chat = new MultiUserChat(mXMPPConnection, room);

		System.out.println("Joining " + room);
		//if (mActivityDescription.isActivityOwner()) {
			try {
				try {
					MultiUserChat.getRoomInfo(mXMPPConnection, room);
					chat.join(mOwner.getActorID(),null,history,10000);
					return chat;
				} catch (Exception e) { /*e.printStackTrace();*/ }
				
				System.out.println("Trying to create room");
				// TODO: is this an error? is there really a notion of ownership?
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
			    submitForm.setAnswer("muc#roomconfig_publicroom", false);
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
}