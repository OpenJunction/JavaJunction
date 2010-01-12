package edu.stanford.junction.api.messaging.target;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.Junction;

public class Session extends MessageTarget {
	
	private Junction jx;

	public Session(Junction jx) {
		this.jx=jx;
	}
	
	@Override
	public void sendMessage(JSONObject message) {
		jx.sendMessageToSession(message);
	}
}