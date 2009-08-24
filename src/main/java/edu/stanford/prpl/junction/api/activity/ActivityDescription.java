package edu.stanford.prpl.junction.api.activity;

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
	 *  that specifies sessionID, switchboard, and requestedRole.
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
	
	private JSONArray rolePlatforms;
	
	private boolean generatedSessionID=false;
	
	public ActivityDescription() {
		sessionID 	= UUID.randomUUID().toString();
		generatedSessionID=true;
		//actorID	 	= UUID.randomUUID().toString();
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
		
		if (json.has("sessionID")) {
			sessionID = json.optString("sessionID");
		} else {
			sessionID = UUID.randomUUID().toString();
			generatedSessionID=true;
		}
		
		////////////////////////////////////////////
		rolePlatforms = json.optJSONArray("roles");
			
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
		} catch (Exception e) {}
		
		mJSON=j;
		return j;
	}
	
	public String[] getRoles() {
		if (rolePlatforms == null) return null;
		String[] roles = new String[rolePlatforms.length()];
		try {
			for (int i=0;i<roles.length;i++) {
				roles[i] = rolePlatforms.getJSONObject(i).getString("role");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return roles;
	}
	
	public JSONObject getRoleSpec(String role) {
		if (rolePlatforms == null) return null;
		try {
			for (int i=0;i<rolePlatforms.length();i++) {
				if (role.equals(rolePlatforms.getJSONObject(i).getString("role"))) {
					return rolePlatforms.getJSONObject(i);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return null;
	}
	
	public JSONObject getRolePlatform(String role, String platform) {
		if (rolePlatforms == null) return null;
		try {
			for (int i = 0; i < rolePlatforms.length();i++) {
				if (role.equals(rolePlatforms.getJSONObject(i).getString("role"))) {
					JSONArray platforms = rolePlatforms.getJSONObject(i).optJSONArray("platforms");
					if (platforms == null) return null;
					for (int j = 0; j < platforms.length(); j++) {
						if (platform.equals(platforms.get(j))) {
							return platforms.getJSONObject(j);
						}
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Platform like: { platform: "android", package: "my.package", url: "market://my.url" }
	 */
	public void addRolePlatform(String role, JSONObject platform) {
		try {
			JSONArray platforms = null;
			if (rolePlatforms==null) rolePlatforms = new JSONArray();
			for (int i = 0; i < rolePlatforms.length();i++) {
				if (role.equals(rolePlatforms.getJSONObject(i).getString("role"))) {
					platforms = rolePlatforms.getJSONObject(i).optJSONArray("platforms");
					if (platforms == null) {
						platforms = new JSONArray();
						rolePlatforms.getJSONObject(i).put("platforms", platforms);
					}
					platforms.put(platform);
					return;
				}
			}
			
			
			// no role found
			JSONObject roleObj = new JSONObject();
			roleObj.put("role", role);
			platforms = new JSONArray();
			roleObj.put("platforms",platforms);
			
			rolePlatforms.put(roleObj);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Example: [ { role: "user", platforms: [ { platform: "android" ... } ] },
	 * 			  { role: "screen", platforms: [ { platform: "web" ... } ] }
	 * 			]
	 * @param platforms
	 */
	public void setRoles(JSONArray roles) {
		rolePlatforms=roles;
	}
}
