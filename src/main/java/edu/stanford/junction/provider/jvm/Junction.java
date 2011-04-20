/*
 * Copyright (C) 2010 Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
				if (mActivityScript == null) {
					triggerActorJoin(false);
				} else {
					triggerActorJoin(mActivityScript.isActivityCreator()); // TODO: wrong. Add presence.
				}
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