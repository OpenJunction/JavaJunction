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
	
	public abstract void loadJSON(JSONObject data) throws JSONException;
	
	// Required for deserialization
	public String getJxMessageType() {
		System.out.println("got " + this.getClass().getGenericSuperclass().toString());
		return this.getClass().getSimpleName();
	}
	
	public static JunctionMessage load(String data) {
		try {
			JunctionMessage message = null;
			
			JSONObject json = new JSONObject(data);
			
			if ("jxquery".equals(json.get("jxMessageType"))) {
				message = new JunctionQuery();
				message.loadJSON(json);
			}
			
			return message;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
}
