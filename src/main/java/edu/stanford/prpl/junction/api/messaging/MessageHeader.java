package edu.stanford.prpl.junction.api.messaging;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.messaging.target.MessageTarget;
import edu.stanford.prpl.junction.api.messaging.target.MessageTargetFactory;
import edu.stanford.prpl.junction.impl.Junction;

public class MessageHeader {
	private Junction jx;
	private JSONObject message;
	
	public MessageHeader(Junction jx, JSONObject message) {
		this.jx=jx;
		this.message=message;
	}
	
	
	public MessageTarget getReplyTarget() {
		if (message.has(Junction.NS_JX)) {
			JSONObject h = message.optJSONObject(Junction.NS_JX);
			if (h.has("replyTo")) {
				return MessageTargetFactory.getInstance(jx).getTarget(h.optString("replyTo"));
			}
		}
		
		return MessageTargetFactory.getInstance(jx).getTarget("session");
	}
	
	public Junction getJunction() { return jx; }
}
