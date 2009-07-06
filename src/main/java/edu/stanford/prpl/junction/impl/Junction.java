package edu.stanford.prpl.junction.impl;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cometd.Client;
import org.cometd.Message;

import edu.stanford.prpl.junction.api.activity.JunctionActor;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;

public class Junction implements edu.stanford.prpl.junction.api.activity.Junction {
	private String mActivityID;
	private String mSessionID;
	private List<JunctionActor>mActors;
	private JunctionManager mManager;
	private URL mHostURL;
	private String sessionChannel;
	
	/**
	 * Creates an activity from a given activity URL
	 */
	public Junction(URL activityURL) {
		// http://prpl.stanford.edu:8181/cometd/cometd?session=XYZ
		try {
			mHostURL = new URL(activityURL.getProtocol() 
								+ "://" 
								+ activityURL.getHost() 
								+ ":"
								+ activityURL.getPort()
								+ activityURL.getPath());
			
			String query = activityURL.getQuery();
			int i = query.indexOf("session=");
			if (i < 0) {
				throw new IllegalArgumentException("No activity session ID found in activity URL " + activityURL);
			}
			i+=8;
			int j = query.indexOf("&",i);
			if (j > 0) {
				mSessionID = query.substring(i, j);
			} else {
				mSessionID = query.substring(i);
			}

		} catch (Exception e) {
			throw new IllegalArgumentException("Could not get Junction server from URL " + activityURL.toExternalForm());
		}
		
		Map<String,Object>params = new HashMap<String, Object>();
		params.put("host",mHostURL);
		params.put("sessionID", mSessionID);
		mManager = new JunctionManager(params);
		
		// set activity ID
		// instantiate mManager
		init();
	}
	
	/**
	 * Creates a new activity and registers it
	 * with a Junction server.
	 * 
	 * TODO: add constructor w/ activity descriptor; keep this one for nonconstrained activity.
	 */
	protected Junction(JunctionManager manager) {
		mManager = manager;
		mHostURL = manager.getHostURL();
		mSessionID = UUID.randomUUID().toString();
		init();
	}
	
	private void init() {
		mActors = new ArrayList<JunctionActor>();
		sessionChannel = "/session/"+mSessionID;
		mManager.addListener(sessionChannel, new OnStartListener());
	}
	
	public String getActivityID() {
		return mActivityID;
	}
	
	
	public void registerActor(final JunctionActor actor) {
		System.out.println("adding actor for role " + actor.getRole());
		mActors.add(actor);
		
		// TODO: formalize this and pair w/ JunctionMaker; 
		// keep mActors synched better w/ JunctionManager using a stateful proxy?
		registerMessageHandler(new MessageHandler() {

			public void onMessageReceived(Client from, Message message) {
				if (message.getChannel().equals(mManager.channelForSystem())) {
					Map<String,Object>data = (Map<String,Object>)message.getData();
					if (data == null) return;
					if (data.containsKey("action") && data.get("action").equals("join")) {
						if (!data.get("actorID").equals(actor.getActorID())) {
							Map<String,Object>resp = new HashMap<String, Object>();
							resp.put("action","join");
							resp.put("status","responding"); // new member; strangers should introduce themselves
							resp.put("role",actor.getRole());
							resp.put("actorID",actor.getActorID());
							sendMessageToSystem(message);
							
							
							
							System.out.println("JOINED: " + data.get("role"));
						}
					}
				}
			}
			
		});
		
		
		Map<String,Object>message = new HashMap<String, Object>();
		message.put("action","join");
		message.put("status","new"); // new member; strangers should introduce themselves
		message.put("role",actor.getRole());
		message.put("actorID",actor.getActorID());
		sendMessageToSystem(message);
	}
	
	
	// TODO: use a URL for the service endpoint? (query == service)
	public void requestService(String role, URL host, String serviceName) {
		System.out.println("inviting actor for role " + role);
		
		Map<String,Object>message = new HashMap<String, Object>();
		//message.put("sessionID",getSessionID());
		//message.put("activityHost",mHostURL);
		message.put("activityURL", getInvitationURL(role));
		message.put("serviceName", serviceName);
		mManager.publish("/srv/ServiceFactory", message);
	}
	
	
	public void start() {
		Map<String,String>go = new HashMap<String,String>();
		go.put("command","run");
		mManager.publish(sessionChannel, go);
	}

	
	
	class OnStartListener implements JunctionListener {
		private boolean started=false;
		public void onMessageReceived(Client from, Message message) {
			if (message.getData() == null || started) return;
			
			for (JunctionActor actor : mActors) {
					actor.onActivityStart();
			}
			started=true;
			// mManager.removeListener(this);
		}
	}



	public List<String> getActorsForRole(String role) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getRoles() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSessionID() {
		return mSessionID;
	}

	public void registerMessageHandler(MessageHandler handler) {
		// TODO: reconcile channel w/ handler
		// will have to rewrite the handling stuff down to the bayeux layer
		mManager.addListener(handler);
	}

	public void sendMessageToActor(String actorID, Map<String,Object> message) {
		mManager.publish(mManager.channelForClient(actorID), message);
		
	}

	public void sendMessageToChannel(String channel, Map<String,Object> message) {
		mManager.publish(channel, message);
		
	}
	
	protected void sendMessageToSystem(Map<String,Object>message) {
		mManager.publish(mManager.channelForSystem(), message);
	}

	public void sendMessageToRole(String role, Map<String,Object> message) {
		mManager.publish(mManager.channelForRole(role), message);
	}

	public void sendMessageToSession(Map<String,Object> message) {
		mManager.publish(mManager.channelForSession(), message);
		
	}

	public URL getInvitationURL() {
		URL invitation = null;
		try {
			// TODO: strip query part from hostURL
			invitation = new URL(mHostURL.toExternalForm() + "?session=" + URLEncoder.encode(getSessionID(),"UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return invitation;
	}

	public URL getInvitationURL(String requestedRole) {
		URL invitation = null;
		try {
			// TODO: strip query part from hostURL
			invitation = new URL(mHostURL.toExternalForm() + "?session=" + URLEncoder.encode(getSessionID(),"UTF-8") + "&requestedRole=" + URLEncoder.encode(requestedRole,"UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return invitation;
	}
	
}


