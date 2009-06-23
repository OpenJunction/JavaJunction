package edu.stanford.prpl.junction.api.activity;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JunctionActivity {
	private String mActivityID;
	private List<JunctionRole>mActors;
	
	public JunctionActivity() {
		mActors = new ArrayList<JunctionRole>();
		mActivityID = UUID.randomUUID().toString();
	}
	
	public String getActivityID() {
		return mActivityID;
	}
	
	
	public void registerActor(String role, JunctionRole actor) {
		mActors.add(actor);
		actor.onActivityJoin(this);
	}
	
	
	public void inviteActor(String role, URL serviceURL) {
		
	}
	
	
	public void start() {
		for (JunctionRole actor : mActors) {
			actor.onActivityStart();
		}
	}
}
