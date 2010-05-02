package edu.stanford.junction.api.activity;

import org.json.JSONObject;

import edu.stanford.junction.api.messaging.MessageHeader;

public abstract class JunctionExtra {
	JunctionActor mParent=null;
	
	/**
	 * Provides access to the associated JunctionActor.
	 * @return
	 */
	public final JunctionActor getActor() {
		return mParent;
	}
	
	/**
	 * This method should only be called internally.
	 * @param actor
	 */
	public void setActor(JunctionActor actor) {
		mParent=actor;
	}
	
	/**
	 * Returns true if the normal event handling should proceed;
	 * Return false to stop cascading.
	 */
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) { return true; }
	public void afterOnMessageReceived(MessageHeader h, JSONObject msg) {}
	
	
	public boolean beforeSendMessageToActor(String actorID, JSONObject msg) { return beforeSendMessage(msg); }
	public boolean beforeSendMessageToRole(String role, JSONObject msg) { return beforeSendMessage(msg); }
	public boolean beforeSendMessageToSession(JSONObject msg) { return beforeSendMessage(msg); }
	
	/**
	 * Convenience method to which, by default, all message sending methods call through.
	 * @param msg
	 * @return
	 */
	public boolean beforeSendMessage(JSONObject msg) { return true; }
	//public boolean afterSendMessage(Header h, Message msg) {}
	
	/**
	 * Called before an actor joins an activity.
	 * Returning false aborts the attempted join.
	 */
	public boolean beforeActivityJoin() {
		return true;
	}
	
	public void afterActivityJoin() {
		
	}
	
	//public void beforeGetActivityScript();
	
	/**
	 * Returns an integer priority for this Extra.
	 * Lower priority means closer to switchboard;
	 * Higher means closer to actor.
	 */
	public Integer getPriority() { return 20; }
}
