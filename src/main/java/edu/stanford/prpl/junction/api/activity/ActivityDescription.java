package edu.stanford.prpl.junction.api.activity;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class ActivityDescription {
	
	/**
	 * Note that there is probably a lot of confusing code in here
	 * due to our redefinition of the Activity Description.
	 * 
	 *  The new setup:: there is an ActivityDescription that details
	 *  roles / platforms / codebases, and a SessionDescription,
	 *  that specifies sessionID, switchboard, and role.
	 *  
	 *  Both of these constructs can be used to instantiate an Actor,
	 *  one by creating a new session and one by joining a previously
	 *  created one.
	 */
	
	// JSON representation
	private JSONObject mJSON = null;
	
	// session tokens
	// TODO: Remove this stuff from this class
	private String sessionID;
	private String host;
	
	private String activityID;
	private String friendlyName;
	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
		mJSON=null; // reset
	}

	private JSONObject rolePlatforms;
	
	private boolean generatedSessionID=false;
	
	public ActivityDescription() {
		sessionID 	= UUID.randomUUID().toString();
		generatedSessionID=true;
	}
	
	public ActivityDescription(JSONObject json) {
		mJSON = json;
		
		
		// TODO: Deprecate. These should not be in the activityDescription.
		// preferred
		if (json.has("switchboard")) {
			host = json.optString("switchboard");
		}
		// deprecated
		else if (json.has("host")) {
			host = json.optString("host");
		}
		
		// TODO: rename this field
		if (json.has(("ad"))) {
			activityID = json.optString("ad");
		}
		
		if (json.has("friendlyName")) {
			friendlyName=json.optString("friendlyName");
		}
		
		if (json.has("sessionID")) {
			sessionID = json.optString("sessionID");
		} else {
			sessionID = UUID.randomUUID().toString();
			generatedSessionID=true;
		}
		
		////////////////////////////////////////////
		rolePlatforms = json.optJSONObject("roles");
			
	}
	
	
	public boolean isActivityCreator() {
		return generatedSessionID;
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
		mJSON=null; // reset
	}
	
	public JSONObject getJSON() {
		// hack until the object is populated correctly
		if (mJSON != null) {
			return mJSON;
		}
		
		JSONObject j = new JSONObject();
		try {
			j.put("sessionID", sessionID);
			j.put("switchboard",host);
			if (rolePlatforms != null) {
				j.put("roles",rolePlatforms);
			}
			if (friendlyName != null) {
				j.put("friendlyName", friendlyName);
			}
		} catch (Exception e) {}
		
		mJSON=j;
		return j;
	}
	
	public String[] getRoles() {
		if (rolePlatforms == null) return new String[]{};
		String[] roles = new String[rolePlatforms.length()];
		
		try {
			Iterator<String>iter = rolePlatforms.keys();
			int i=0;
			while (iter.hasNext()) {
				roles[i++] = iter.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return roles;
	}
	
	public JSONObject getRoleSpec(String role) {
		if (rolePlatforms == null) return null;
		if (rolePlatforms.has(role)) {
			try {
				return rolePlatforms.getJSONObject(role);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public JSONObject getRolePlatform(String role, String platform) {
		if (rolePlatforms == null) return null;
		try {
			JSONObject spec = getRoleSpec(role);
			if (spec == null) return null;
			if (spec.has(platform)) {
				return spec.getJSONObject(platform);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Platform like: { platform: "android", package: "my.package", url: "market://my.url" }
	 */
	public void addRolePlatform(String role, String platform, JSONObject platformSpec) {
		try {
			JSONArray platforms = null;
			if (rolePlatforms==null) rolePlatforms = new JSONObject();
			if (!rolePlatforms.has(role)) {
				rolePlatforms.put(role, new JSONObject());
			}
			
			JSONObject jsonRole = rolePlatforms.getJSONObject(role);
			if (!jsonRole.has("platforms")) {
				jsonRole.put("platforms", new JSONObject());
			}
			
			JSONObject jsonPlatforms = jsonRole.getJSONObject("platforms");
			jsonPlatforms.put(platform, platformSpec);
			
			mJSON=null; // reset
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setRoles(JSONObject roles) {
		rolePlatforms=roles;
	}
}
