package edu.stanford.prpl.junction.api.activity;

import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;

public class ActivityDescription {
	
	/**
	 * Note that there is probably a lot of confusing code in here
	 * due to our redefinition of the Activity Description.
	 * 
	 *  The new setup:: there is an ActivityDescription that details
	 *  roles / platforms / codebases, and a SessionDescription,
	 *  that specifies sessionID, switchboard, and requestedRole.
	 *  
	 *  Both of these constructs can be used to instantiate an Actor,
	 *  one by creating a new session and one by joining a previously
	 *  created one.
	 */
	
	// JSON representation
	private JSONObject mJSON = null;
	
	// session tokens
	private String sessionID;
	private String host;
	private String activityID;
	
	// member tokens
	private boolean isActivityCreator;
	//private String actorID;
	private String[] actorRoles = {};
	
	public ActivityDescription() {
		sessionID 	= UUID.randomUUID().toString();
		//actorID	 	= UUID.randomUUID().toString();
	}
	
	public ActivityDescription(JSONObject json) {
		mJSON = json;
		
		// preferred
		if (json.has("switchboard")) {
			host = json.optString("switchboard");
		}
		// deprecated
		else if (json.has("host")) {
			host = json.optString("host");
		}
		
		
		if (json.has("sessionID")) {
			sessionID = json.optString("sessionID");
		} else {
			isActivityCreator=true;
			sessionID = UUID.randomUUID().toString();
		}
		/*
		if (json.has("actorID")) {
			actorID = json.optString("actorID");
		} else {
			actorID = UUID.randomUUID().toString();
		}*/
	}
	
	public ActivityDescription(Map<String,Object>desc) {
		if (desc.containsKey("host")) {
			host = (String)desc.get("host");
		}
		
		
		if (desc.containsKey("sessionID")) {
			sessionID = (String)desc.get("sessionID");
			// probably temporary?
			if (!desc.containsKey("owner")) {
				isActivityCreator=false;
			} else {
				isActivityCreator=true;
			}
		} else {
			isActivityCreator=true;
			sessionID = UUID.randomUUID().toString();
		}
		
		if (desc.containsKey("activityID")) {
			activityID = (String)desc.get("activityID");
		}
		/*
		if (desc.containsKey("actorID")) {
			actorID = (String)desc.get("actorID");
		} else {
			actorID = UUID.randomUUID().toString();
		}*/
	}
	
	/*
	public String getActorID() {
		return actorID;
	}
	


	public void setActorID(String actorID) {
		this.actorID = actorID;
	}
   */
	
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getActivityID() {
		return activityID;
	}
	public void setActivityID(String activityID) {
		this.activityID = activityID;
	}
	
	public void setActorRoles(String[] roles) {
		actorRoles=roles;
	}
	
	public String[] getActorRoles() {
		return actorRoles;
	}
	
	public boolean isActivityCreator() {
		return isActivityCreator;
	}
	
	public JSONObject getJSON() {
		return mJSON;
	}
}
