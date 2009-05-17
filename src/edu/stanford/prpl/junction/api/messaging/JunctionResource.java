package edu.stanford.prpl.junction.api.messaging;

import org.json.JSONException;
import org.json.JSONObject;

public class JunctionResource extends JunctionMessage {

	@Override
	public String getJxMessageType() {
		return "jxresource";
	}

	@Override
	public void loadJSON(JSONObject data) throws JSONException {
	
	}

}
