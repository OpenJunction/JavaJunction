package edu.stanford.prpl.junction.api.messaging.target;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.activity.Junction;

public class Role extends MessageTarget {
	private Junction jx;
	public String role;
	public Role(Junction jx, String r) {
		this.jx=jx;
		role=r;
	}
	
	@Override
	public void sendMessage(JSONObject message) {
		jx.sendMessageToRole(role, message);
	}
}