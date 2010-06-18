package edu.stanford.junction;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHandler;

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
	//public void registerMessageHandler(MessageHandler handler);
	
	// TODO: get rid of this; have all the glue be implementation-agnostic
	// as much as possible
	public abstract void registerExtra(JunctionExtra extra);
}
