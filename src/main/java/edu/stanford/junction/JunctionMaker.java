package edu.stanford.junction;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONObject;


import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.JunctionProvider;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;


public class JunctionMaker {
	protected JunctionProvider mProvider;
	
	public static JunctionMaker getInstance(SwitchboardConfig switchboardConfig) {
		// TODO: map config to maker?
		JunctionMaker maker = new JunctionMaker();
		maker.mProvider = maker.getProvider(switchboardConfig);
		maker.mProvider.setJunctionMaker(maker);
		return maker;
	}
	
	protected JunctionMaker() {
		
	}
	
	
	protected JunctionProvider getProvider(SwitchboardConfig switchboardConfig) {
		if (switchboardConfig instanceof XMPPSwitchboardConfig) {
			return new edu.stanford.junction.provider.xmpp.JunctionProvider((XMPPSwitchboardConfig)switchboardConfig);
		} else {
			// Unknown implementation;.
			return null;
		}
	}
	
	/*
	 * JunctionMaker has three functions:
	 * (1) Connect an Actor to a Junction
	 * (2) Retrieve an activity's script given a URI
	 * (3) Support various invitation mechanisms (often platform-specific)
	 */
	public Junction newJunction(URI uri, JunctionActor actor) {
		return mProvider.newJunction(uri, actor);
	}
	
	public Junction newJunction(ActivityScript desc, JunctionActor actor) {
		return mProvider.newJunction(desc, actor);
	}
	
	public ActivityScript getActivityScript(URI uri) {
		return mProvider.getActivityScript(uri);
	}
	
	/**
	 * Sends a request to a Director activity
	 * to cast an actor to accept a given invitation.
	 * 
	 * @param directorURI The director listening for requests
	 * @param invitationURI The activity to join (potentially including role information)
	 */
	public void castActor(final URI directorURI, final URI invitationURI) {
		JunctionActor actor = new JunctionActor("inviter") {
			@Override
			public void onActivityJoin() {
				JSONObject invitation = new JSONObject();
				try {
					invitation.put("action","cast");
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
		
		JunctionMaker.this.newJunction(directorURI, actor);
	}
	
	/**
	 * Returns the role associated with a given Junction invitation.
	 * @param uri
	 * @return
	 */
	public static String getRoleFromInvitation(URI uri) {
		String query = uri.getQuery();
		if (query == null) return null;
		int pos = query.indexOf("role=");
		if (pos == -1) {
			return null;
		}

		query = query.substring(pos+5);
		pos = query.indexOf('&');
		if (pos > -1) {
			query = query.substring(0,pos);
		}
		return query;
	}
	
	@Deprecated
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
	@Deprecated
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
			JunctionMaker.this.newJunction(remoteServiceActivity, actor);
	}
}
