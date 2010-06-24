package edu.stanford.junction.provider.jvm;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;

public class Junction extends edu.stanford.junction.Junction {
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
				triggerActorJoin(mActivityScript.isActivityCreator()); // TODO: wrong
			};
		}.start();
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
	public URI getBaseInvitationURI() {
		try {
			return new URI("junction://localhost#jvm");
		} catch (Exception e) {
			return null;
		}
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
	public void doSendMessageToActor(String actorID, JSONObject message) {
		multiJunction.sendMessageToActor(mOwner,actorID,message);
	}

	@Override
	public void doSendMessageToRole(String role, JSONObject message) {
		multiJunction.sendMessageToRole(mOwner,role,message);
	}

	@Override
	public void doSendMessageToSession(JSONObject message) {
		multiJunction.sendMessageToSession(mOwner,message);
	}

	@Override
	public JunctionActor getActor() {
		return mOwner;
	}
	
}