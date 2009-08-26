package edu.stanford.prpl.junction.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.json.JSONException;
import org.json.JSONObject;


import edu.stanford.prpl.junction.api.activity.ActivityDescription;
import edu.stanford.prpl.junction.api.activity.JunctionActor;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.api.messaging.MessageHeader;


public class JunctionMaker {
	private String mSwitchboard;
	
	public static JunctionMaker getInstance(String switchboard) {
		// todo: singleton per-URL?
		return new JunctionMaker(switchboard);
	}
	
	public static JunctionMaker getInstance() {
		return new JunctionMaker();
	}
	
	protected JunctionMaker() {
		
	}
	
	protected JunctionMaker(String switchboard) {
		mSwitchboard=switchboard;
	}
	
	// TODO: add 0-arg constructor for activities w/ given junction hosts
	// or allow static call for newJunction?
	
	public Junction newJunction(String friendlyName, JunctionActor actor) {
		return null;
	}
	
	public Junction newJunction(URI uri, JunctionActor actor) {
		ActivityDescription desc = new ActivityDescription();
		desc.setHost(uri.getHost());
		if (uri.getPath() != null) { // TODO: check to make sure this works for URIs w/o path
			desc.setSessionID(uri.getPath().substring(1));
		}
		
		return newJunction(desc,actor);
		
		/*
		  
		ActivityDescription desc = new ActivityDescription();
		String query = url.getQuery();
		
		String tmp;
		if (null != (tmp = getURLParam(query,"sessionID"))) {
			desc.setSessionID(tmp);
		}
		
		desc.setHost(url.getHost());
		//desc.setActorID(actor.getActorID());
		
		return newJunction(desc,actor);
		
		*/
	}
	
	private String getURLParam(String query, String param) {
		int pos = query.indexOf(param+"=");
		if (pos < 0) return null;
		
		String val = query.substring(pos+1+param.length());
		pos = val.indexOf("&");
		if (pos > 0)
			val = val.substring(0,pos);
		
		return val;
	}
	
	public Junction newJunction(ActivityDescription desc, JunctionActor actor) {
		
		// this needs to be made more formal
		if (null == desc.getHost() && mSwitchboard != null) {
			desc.setHost(mSwitchboard);
		}
		
		Junction jx = new Junction(desc);
		jx.registerActor(actor);
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return jx;
	}
	
	
	public void inviteActorByListenerService(final URI invitationURI, URI listenerServiceURI) {
		JunctionActor actor = new JunctionActor("inviter") {
			@Override
			public void onActivityJoin() {
				JSONObject invitation = new JSONObject();
				try {
					invitation.put("activity", invitationURI.toString());
				} catch (Exception e) {}
				getJunction().sendMessageToSession(invitation);
				leave();
			}
		};
		
		JunctionMaker.getInstance().newJunction(listenerServiceURI, actor);
	}
	
	/**
	 * Requests a listening service to join this activity as the prescribed role. Here,
	 * the service must be detailed in the activity description's list of roles.
	 * 
	 * An example platform in the role specification:
	 * { role: "dealer", platforms: [ 
	 * 						{ platform: "jxservice", 
	 * 						  classname: "edu.stanford.prpl.poker.dealer",
	 * 						  switchboard: "my.foreign.switchboard" } ] }
	 * 
	 * If switchboard is not present, it is assumed to be on the same switchboard
	 * on which this activity is being run.
	 * 
	 * @param role
	 * @param host
	 * @param serviceName
	 */
	public void inviteActorService(final URI invitationURI) {

		ActivityDescription desc = getActivityDescription(invitationURI);
		System.out.println("Desc: " + desc.getJSON().toString());
		// find service platform spec
		int i;
		String role = invitationURI.toString();
		if ((i=role.indexOf("requestedRole=")) >= 0) {
			role = role.substring(i+14);
			if ((i=role.indexOf("&")) >= 0) {
				role = role.substring(0,i);
			}
			System.out.println("inviting service for role " + role);
			
			JSONObject platform = desc.getRolePlatform(role, "jxservice");
			System.out.println("got platform " + platform);
			if (platform == null) return;
			
			String switchboard = platform.optString("switchboard");
			if (switchboard == null) {
				switchboard = invitationURI.getHost();
			}
			final String serviceName = platform.optString("serviceName");
			
			// // // // // // // // // // // // // // // // 
			JunctionActor actor = new JunctionActor("inviter") {
				@Override
				public void onActivityJoin() {
					JSONObject invitation = new JSONObject();
					try {
						invitation.put("activity", invitationURI.toString());
						invitation.put("serviceName",serviceName);
					} catch (Exception e) {}
					getJunction().sendMessageToSession(invitation);
					leave();
				}
			};
			
			
			// remote jxservice activity:
			URI remoteServiceActivity=null;
			try {
				remoteServiceActivity = new URI("junction://"+switchboard+"/jxservice");
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return;
			}
			System.out.println("Inviting serice at uri " + remoteServiceActivity);
			JunctionMaker.getInstance().newJunction(remoteServiceActivity, actor);
		}
	}
	
	
	public ActivityDescription getActivityDescription(URI uri) {
		
		// TODO: Move the XMPPConnection into the JunctionMaker
		// (out of Junction)
		/*
		JunctionMaker jm = null;
		String host = uri.getHost();
		if (host.equals(mSwitchboard)) {
			jm = this;
		} else {
			jm = new JunctionMaker(host);
		}
		 */
		
		String host = uri.getHost();
		String sessionID = uri.getPath().substring(1);
		XMPPConnection conn = getXMPPConnection(host);
		
		String room = sessionID+"@conference."+host;
		System.err.println("looking up info from xmpp room " + room);
		try {
			RoomInfo info = MultiUserChat.getRoomInfo(conn, room);
			String descString = info.getDescription();
			JSONObject descJSON = new JSONObject(descString);
			
			return new ActivityDescription(descJSON);
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private XMPPConnection getXMPPConnection(String host) {
		XMPPConnection mXMPPConnection= new XMPPConnection(host);
		try {
			mXMPPConnection.connect();
			mXMPPConnection.loginAnonymously();
			
			return mXMPPConnection;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
