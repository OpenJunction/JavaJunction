package edu.stanford.junction;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONObject;


import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;


public abstract class JunctionMaker {
	
	// TODO: Have a single getInstance method that takes a SwitchboardConfig
	
	public static JunctionMaker getInstance(String switchboard) {
		// TODO: map config to maker?
		return new edu.stanford.junction.impl.xmpp.JunctionMaker(switchboard);
	}
	
	public static JunctionMaker getInstance() {
		return new edu.stanford.junction.impl.xmpp.JunctionMaker();
	}
	
	public JunctionMaker() {
		
	}
	
	/*
	 * JunctionMaker has three functions:
	 * (1) Connect an Actor to a Junction
	 * (2) Retrieve an activity's script given a URI
	 * (3) Support various invitation mechanisms (often platform-specific)
	 */
	public abstract Junction newJunction(URI uri, JunctionActor actor);
	public abstract Junction newJunction(ActivityScript desc, JunctionActor actor);
	public abstract ActivityScript getActivityDescription(URI uri);
	
	
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
			
			@Override
			public void onMessageReceived(MessageHeader header,
					JSONObject message) {
				
			}
		};
		
		JunctionMaker.this.newJunction(listenerServiceURI, actor);
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
	ActivityScript desc = jx.getActivityDescription();
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
				
				@Override
				public void onMessageReceived(MessageHeader header,
						JSONObject message) {
					
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
			System.out.println("Inviting service at uri " + remoteServiceActivity);
			JunctionMaker.getInstance().newJunction(remoteServiceActivity, actor);
		
	}
}
