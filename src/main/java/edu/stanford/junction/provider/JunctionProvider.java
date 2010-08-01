package edu.stanford.junction.provider;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.Cast;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public abstract class JunctionProvider {
	protected JunctionMaker mJunctionMaker;
	
	public abstract Junction newJunction(URI uri, ActivityScript script, JunctionActor actor);


	/**
	 * Unoptimized implementation that joins a session,
	 * sends a messasge, and disconnects
	 */
	public void sendMessageToActivity(URI activitySession, final JSONObject msg) {
		JunctionActor sender = new JunctionActor("transient") {
			@Override
			public void onMessageReceived(MessageHeader header,
					JSONObject message) {
			}
			
			@Override
			public void onActivityJoin() {
				sendMessageToSession(msg);
				leave();
			}
		};
		
		newJunction(activitySession, null, sender);
	}
	
	
	//public abstract Junction newJunction(ActivityScript desc, JunctionActor actor, Cast support);
	
	public abstract ActivityScript getActivityScript(URI uri);
	
	public abstract URI generateSessionUri();
	
	public void setJunctionMaker(JunctionMaker maker) {
		this.mJunctionMaker=maker;
	}
	
	@Deprecated
	protected void requestServices(Junction jx, ActivityScript desc) {
		if (desc.isActivityCreator()) {
			String[] roles = desc.getRoles();
			for (String role : roles) {
				System.out.println("roles:" + role);
				JSONObject plat = desc.getRolePlatform(role, "jxservice");
				System.out.println("plat:" + plat);
				if (plat != null) {
					// Auto-invite the service via the service factory
					System.out.println("Auto-requesting service for " + role);
					mJunctionMaker.inviteActorService(jx,role);
					// TODO: add a method that takes in a Junction
					// so we don't have to do an extra lookup
				}
			}
		}
	}
}
