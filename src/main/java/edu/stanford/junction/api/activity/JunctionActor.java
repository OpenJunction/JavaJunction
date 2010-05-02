package edu.stanford.junction.api.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

// TODO: Make interface along with abstract class, for those who really need it.

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

	public void sendMessageToActor(String actorID, JSONObject message) {
		mJunction.sendMessageToActor(actorID, message);
	}
	
	public void sendMessageToSession(JSONObject message) {
		mJunction.sendMessageToSession(message);
	}
	
	public void sendMessageToRole(String role, JSONObject message) {
		mJunction.sendMessageToRole(role, message);
	}
	
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
