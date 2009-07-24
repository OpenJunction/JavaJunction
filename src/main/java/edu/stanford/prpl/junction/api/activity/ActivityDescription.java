package edu.stanford.prpl.junction.api.activity;

import java.util.Map;
import java.util.UUID;

public class ActivityDescription {
	// session tokens
	private String sessionID;
	private String host;
	private String activityID;
	
	// member tokens
	private boolean isActivityOwner;
	private String actorID;
	private String[] actorRoles = {};
	
	public ActivityDescription(Map<String,Object>desc) {
		if (desc.containsKey("host")) {
			host = (String)desc.get("host");
		}
		
		
		if (desc.containsKey("sessionID")) {
			sessionID = (String)desc.get("sessionID");
			// probably temporary?
			if (!desc.containsKey("owner")) {
				isActivityOwner=false;
			} else {
				isActivityOwner=true;
			}
		} else {
			isActivityOwner=true;
			sessionID = UUID.randomUUID().toString();
		}
		
		if (desc.containsKey("activityID")) {
			activityID = (String)desc.get("activityID");
		}
		
		if (desc.containsKey("actorID")) {
			actorID = (String)desc.get("actorID");
		} else {
			actorID = UUID.randomUUID().toString();
		}
	}
	
	
	public String getActorID() {
		return actorID;
	}


	public void setActorID(String actorID) {
		this.actorID = actorID;
	}


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
	
	public boolean isActivityOwner() {
		return isActivityOwner;
	}
}
