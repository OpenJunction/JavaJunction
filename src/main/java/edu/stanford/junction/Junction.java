package edu.stanford.junction;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.ExtrasDirector;

public abstract class Junction {
	public static String NS_JX = "jx";
	
	/**
	 * Required constructors
	 */
	public Junction() {}
	
	
	/** 
	 * Activity Description
	 */
	
	public abstract ActivityScript getActivityScript();
	
	public abstract URI getAcceptedInvitation();
	
	//public String getActivityID();
	public abstract String getSessionID();
	
	// TODO: doesn't make sense for other switchboard implementations.
	// return Maker object instead?
	@Deprecated
	public abstract String getSwitchboard();
	//public String[] getRoles();
	
	/**
	 * Activity Lifecycle
	 */
	//public abstract void start();
	public abstract void disconnect();
	// public abstract void onStart(); // or register handler?
	
	/** 
	 * Actor Management
	 */
	
	//public abstract void registerActor(JunctionActor actor);
	//public List<String> getActorsForRole(String role);
	// getActorsForHuman(String id); // some way of getting actor(s) associated with a person
	//public void onActorJoin(JunctionActor actor); // or do we want registerActorJoinHandler()
	public abstract JunctionActor getActor();
	
	/**
	 * Actor Invitation
	 */
	public abstract URI getInvitationURI();
	public abstract URI getInvitationURI(String role);
	// there will also be device-specific methods, EG QR codes / contact list on Android
	
	/**
	 * Messaging
	 */
	
	// send
	public abstract void sendMessageToRole(String role, JSONObject message);
	public abstract void sendMessageToActor(String actorID, JSONObject message);
	public abstract void sendMessageToSession(JSONObject message);
	
	// receive
	public void triggerMessageReceived(MessageHeader header, JSONObject message) {
		if (mExtrasDirector.beforeOnMessageReceived(header,message)) {
			getActor().onMessageReceived(header, message);
			mExtrasDirector.afterOnMessageReceived(header,message);
		}
	}
	
	public void triggerActorJoin(boolean isCreator) {
		// Create
		if (isCreator) {
			if (!mExtrasDirector.beforeActivityCreate()) {
				disconnect();
				return;
			}
			getActor().onActivityCreate();
			mExtrasDirector.afterActivityCreate();
		}
		
		// Join
		if (!mExtrasDirector.beforeActivityJoin()) {
			disconnect();
			return;
		}
		getActor().onActivityJoin();
		mExtrasDirector.afterActivityJoin();
	}
	
	private ExtrasDirector mExtrasDirector = new ExtrasDirector();
	public void registerExtra(JunctionExtra extra) {
		extra.setActor(getActor());
		mExtrasDirector.registerExtra(extra);
	}
	
	// TODO: get rid of this; be smarter about what you expose.
	// <actor> :: JUNCTION :: <junction provider>
	public ExtrasDirector getExtrasDirector() {
		return mExtrasDirector;
	}
}
