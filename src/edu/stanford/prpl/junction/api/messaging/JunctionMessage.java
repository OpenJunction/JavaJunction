package edu.stanford.prpl.junction.api.messaging;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JunctionMessage {
	
	// Bayeux fundamentals
	public JSONObject toJSON() {
		try {
			JSONObject obj = new JSONObject(this);
			obj.put("jxMessageType",getJxMessageType());
			
			return obj;
		} catch (JSONException e) {
			// Exception caught here; JunctionMessages
			// need to create clean JSON.
			e.printStackTrace();
			
			return null;
		}
		
	}
	
	public void loadJSON(JSONObject data) {
		System.out.println("JunctionMessage.loadJSON needs to be written");
	}
	
	// Required for deserialization
	public String getJxMessageType() {
		System.out.println("got " + this.getClass().getGenericSuperclass().toString());
		return this.getClass().getSimpleName();
	}
}
