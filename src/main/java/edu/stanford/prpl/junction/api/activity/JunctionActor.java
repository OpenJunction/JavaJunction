package edu.stanford.prpl.junction.api.activity;

import java.util.UUID;

import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.impl.Junction;

public abstract class JunctionActor {
	protected Junction mJunction;
	private String actorID;
	private String mRole;
	
	public String getRole() {
		return mRole;
	}
	
	public JunctionActor(String role) {
		actorID = UUID.randomUUID().toString();
		mRole = role;
	}
	
	public final void join(Junction junction) {
		mJunction = junction;
		MessageHandler handler = getMessageHandler();
		if (handler != null) {
			junction.registerMessageHandler(handler);
		}

		junction.registerActor(this);
		onActivityJoin(junction);
	}
	
	public void onActivityJoin(Junction activity) {
		
	}
	
	public Junction getJunction() {
		return mJunction;
	}
	
	public String getActorID() {
		return actorID;
	}
	
	public abstract void onActivityStart(); 
	
	// TODO: yes, this should be a MessageHandler
	public MessageHandler getMessageHandler() { return null; }
}
