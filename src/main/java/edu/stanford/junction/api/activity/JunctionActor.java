package edu.stanford.junction.api.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * The JunctionActor is the primary class used
 * to communicate within a Junction activity.
 * <br/><br/>
 * Primarily, you will need to override the onMessageReceived
 * method to handle inbound messages, and use the sendMessageTo*
 * methods to transmit them.
 */
public abstract class JunctionActor {
	protected Junction mJunction;
	private String actorID;
	private String[] mRoles;
	
	public String[] getRoles() {
		return mRoles;
	}
	
	public JunctionActor(String role) {
		actorID = UUID.randomUUID().toString();
		mRoles = new String[]{role};
	}
	
	public JunctionActor(String[] roles) {
		actorID = UUID.randomUUID().toString();
		mRoles=roles;
	}
	
	public void onActivityJoin() {
		
	}
	
	public void setJunction(Junction j) {
		mJunction=j;
	}
	
	public Junction getJunction() {
		return mJunction;
	}
	
	public String getActorID() {
		return actorID;
	}
	
	public void onActivityStart() {
		
	}
	
	public final void leave() {
		Junction jx = getJunction();
		if (jx != null) {
			jx.disconnect();
			setJunction(null);
		}
	}
	
	public void onActivityCreate() {
		
	}

	/**
	 * Send a message to an individual actor, idenfified by actorID
	 */
	public final void sendMessageToActor(String actorID, JSONObject message) {
		mJunction.sendMessageToActor(actorID, message);
	}

	/**
	 * Send a message for anyone in the Junction session.
	 */	
	public final void sendMessageToSession(JSONObject message) {
		mJunction.sendMessageToSession(message);
	}
	
	/**
	 * Send a message to an actor claiming a certain role.
	 */
	public final void sendMessageToRole(String role, JSONObject message) {
		mJunction.sendMessageToRole(role, message);
	}
	
	/**
	 * Asynchronously handle an inbound message.
	 */
	public abstract void onMessageReceived(MessageHeader header, JSONObject message);
	
	/**
	 * Returns a list of JunctionExtras that should be loaded
	 * when the actor joins an activity
	 * @return
	 */
	public List<JunctionExtra>getInitialExtras() {
		return new ArrayList<JunctionExtra>();
	}
	
	public void registerExtra(JunctionExtra extra) {
		mJunction.registerExtra(extra);
	}
}
