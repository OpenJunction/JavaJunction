package edu.stanford.junction.provider.jvm;

import java.net.URI;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;

public class Junction extends edu.stanford.junction.Junction {
	private static Map<String,Map<String,JunctionActor>> junctions;
	private JunctionActor mOwner;
	private ActivityScript mActivityScript;
	MultiJunction multiJunction;
	
	public Junction(URI uri, ActivityScript script, final JunctionActor actor) {
		mActivityScript = script;
		multiJunction = MultiJunction.get(uri,actor);
		mOwner = actor;
		new Thread() {
			public void run() {
				actor.setJunction(Junction.this);
				actor.onActivityJoin();
			};
		}.start();
		
		// TODO: check for onActivityCreate
	}
	
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public URI getAcceptedInvitation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ActivityScript getActivityScript() {
		return mActivityScript;
	}

	@Override
	public URI getInvitationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getInvitationURI(String role) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSessionID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSwitchboard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendMessageToActor(String actorID, JSONObject message) {
		multiJunction.sendMessageToActor(mOwner,actorID,message);
	}

	@Override
	public void sendMessageToRole(String role, JSONObject message) {
		multiJunction.sendMessageToRole(mOwner,role,message);
	}

	@Override
	public void sendMessageToSession(JSONObject message) {
		multiJunction.sendMessageToSession(mOwner,message);
	}

	@Override
	public void registerExtra(JunctionExtra extra) {
		// TODO Auto-generated method stub
		
	}
	
}