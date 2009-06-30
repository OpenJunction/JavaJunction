package edu.stanford.prpl.junction.api.activity;

import java.util.UUID;

import edu.stanford.prpl.junction.impl.Junction;

public abstract class JunctionActor {
	protected Junction mJunction;
	private String actorID;
	
	
	public JunctionActor() {
		actorID = UUID.randomUUID().toString();
	}
	
	public final void join(Junction junction, String role) {
		mJunction = junction;
		junction.registerActor(role, this);
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
}
