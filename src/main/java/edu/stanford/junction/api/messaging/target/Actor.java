package edu.stanford.junction.api.messaging.target;

import org.json.JSONObject;

import edu.stanford.junction.Junction;

public class Actor extends MessageTarget {
		private Junction jx;
		public String id;
		public Actor(Junction jx, String actor) {
			this.jx=jx;
			id=actor;
		}
		
		@Override
		public void sendMessage(JSONObject message) {
			jx.sendMessageToActor(id, message);
		}
	}