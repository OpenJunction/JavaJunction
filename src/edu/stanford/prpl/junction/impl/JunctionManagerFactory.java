package edu.stanford.prpl.junction.impl;

import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionFactory;

public class JunctionManagerFactory implements JunctionFactory {
	// TODO: I should probably be a singleton per-junction session.
	// So if the JSONObject activity is the same, return the same object.
	
	// For testing, use the same JM.
	static JunctionManager mInstance;

	public JunctionAPI create(JSONObject activity) {
		if (mInstance == null) {
			mInstance = new JunctionManager(activity);
		}
		
		return mInstance;
	}

	public JunctionAPI create(URL url) {
		if (mInstance == null) {
			JSONObject desc = new JSONObject();
			try {
				desc.put("host",url.toExternalForm());
			} catch (JSONException e) {
				throw new IllegalArgumentException("Malformed host URL");
			}
			mInstance = new JunctionManager(desc);
		}
		
		return mInstance;
	}

}
