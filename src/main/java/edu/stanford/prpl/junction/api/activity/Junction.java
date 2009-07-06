package edu.stanford.prpl.junction.api.activity;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.cometd.Message;

import edu.stanford.prpl.junction.api.messaging.MessageHandler;

public interface Junction {
	
	/** 
	 * Activity Description
	 */
	
	public String getActivityID();
	public String getSessionID();
	public List<String> getRoles();
	
	/**
	 * Activity Lifecycle
	 */
	public void start();
	// public void onStart(); // or register handler?
	
	/** 
	 * Actor Management
	 */
	
	public void registerActor(JunctionActor actor);
	public List<String> getActorsForRole(String role);
	// getActorsForHuman(String id); // some way of getting actor(s) associated with a person
	//public void onActorJoin(JunctionActor actor); // or do we want registerActorJoinHandler()
	
	/**
	 * Actor Invitation
	 */
	public void requestService(String role, URL host, String serviceName);
	public URL getInvitationURL();
	public URL getInvitationURL(String requestedRole);
	// there will also be device-specific methods, EG QR codes / contact list on Android
	
	/**
	 * Messaging
	 */
	
	// send
	public void sendMessageToChannel(String channel, Map<String,Object> message);
	public void sendMessageToRole(String role, Map<String,Object> message);
	public void sendMessageToActor(String actorID, Map<String,Object> message);
	public void sendMessageToSession(Map<String,Object> message);
	
	// receive
	public void registerMessageHandler(MessageHandler handler);
	
}
