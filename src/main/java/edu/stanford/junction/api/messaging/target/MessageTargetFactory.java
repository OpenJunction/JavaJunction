package edu.stanford.junction.api.messaging.target;

import edu.stanford.junction.api.activity.Junction;

public class MessageTargetFactory {
	private Junction jx;
	
	public static MessageTargetFactory getInstance(Junction jx) {
		return new MessageTargetFactory(jx);
	}
	
	private MessageTargetFactory(Junction jx) {
		this.jx=jx;
	}
	
	public MessageTarget getTarget(String target) {
		if (target.equals("session")) {
			return new Session(jx);
		}
		
		if (target.startsWith("actor:")) {
			return new Actor(jx,target.substring(6));
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
}
