package edu.stanford.prpl.junction.api.messaging.target;

import org.json.JSONObject;

public abstract class MessageTarget {
	public abstract void sendMessage(JSONObject message);
}
