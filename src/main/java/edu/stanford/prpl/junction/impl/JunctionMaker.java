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
	
	public Junction newJunction(URI uri, JunctionActor actor) {
		ActivityDescription desc = new ActivityDescription();
		desc.setHost(uri.getHost());
		if (uri.getPath() != null) { // TODO: check to make sure this works for URIs w/o path
			desc.setSessionID(uri.getPath().substring(1));
		}
		
		return newJunction(desc,actor);
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
		
		if (desc.isActivityCreator()) {
			String[] roles = desc.getRoles();
			for (String role : roles) {
				JSONObject plat = desc.getRolePlatform(role, "jxservice");
				if (plat != null) {
					// Auto-invite the service via the service factory
					System.out.println("Auto-requesting service for " + role);
					inviteActorService(jx,role);
					// TODO: add a method that takes in a Junction
					// so we don't have to do an extra lookup
					
					/*
					URI listenerURI = null;
					try {
						String listener = "junction://" + plat.getString("switchboard")  + "/jxservice";
						listenerURI = new URI(listener);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					inviteActorByListenerService(invitationURI, listenerURI);
					*/
					
				}
			}
		}
		
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
//	public void inviteActorService(final URI invitationURI) {
	public void inviteActorService(final Junction jx, final String role) {
	ActivityDescription desc = jx.getActivityDescription();
		System.out.println("Desc: " + desc.getJSON().toString());
		// find service platform spec
		
			System.out.println("inviting service for role " + role);
			
			JSONObject platform = desc.getRolePlatform(role, "jxservice");
			System.out.println("got platform " + platform);
			if (platform == null) return;
			
			String switchboard = platform.optString("switchboard");
			System.out.println("switchboard: " + switchboard);
			if (switchboard == null || switchboard.length() == 0) {
				switchboard = jx.getSwitchboard();
				System.out.println("switchboard is null, new: " + switchboard);
			}
			final String serviceName = platform.optString("serviceName");
			
			// // // // // // // // // // // // // // // // 
			JunctionActor actor = new JunctionActor("inviter") {
				@Override
				public void onActivityJoin() {
					JSONObject invitation = new JSONObject();
					try {
						invitation.put("activity", jx.getInvitationURI(role));
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
			System.err.println("room desc " + info.getDescription());
			System.err.println("room subj " + info.getSubject());
			System.err.println("part " + info.getOccupantsCount());
			System.err.println("room " + info.getRoom());
			
			String descString = info.getDescription();
			if (descString == null || descString.trim().length()==0) return null;
			
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
