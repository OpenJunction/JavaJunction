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


package edu.stanford.junction.provider;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.Cast;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public abstract class JunctionProvider {
	protected JunctionMaker mJunctionMaker;
	private static boolean DBG = false;

	public abstract Junction newJunction(URI uri, ActivityScript script, JunctionActor actor) throws JunctionException;

	/**
	 * Unoptimized implementation that joins a session,
	 * sends a messasge, and disconnects
	 */
	public void sendMessageToActivity(URI activitySession, final JSONObject msg) throws JunctionException{
		JunctionActor sender = new JunctionActor("transient") {
				@Override
				public void onMessageReceived(MessageHeader header,
											  JSONObject message) {
				}
			
				@Override
				public void onActivityJoin() {
					sendMessageToSession(msg);
					leave();
				}
			};
		newJunction(activitySession, null, sender);
	}
	
	
	//public abstract Junction newJunction(ActivityScript desc, JunctionActor actor, Cast support);
	
	public abstract ActivityScript getActivityScript(URI uri) throws JunctionException;
	
	public abstract URI generateSessionUri();
	
	public void setJunctionMaker(JunctionMaker maker) {
		this.mJunctionMaker=maker;
	}
	
	@Deprecated
	protected void requestServices(Junction jx, ActivityScript desc) throws JunctionException{
		if (desc.isActivityCreator()) {
			String[] roles = desc.getRoles();
			for (String role : roles) {
				if (DBG) System.out.println("roles:" + role);
				JSONObject plat = desc.getRolePlatform(role, "jxservice");
				if (DBG) System.out.println("plat:" + plat);
				if (plat != null) {
					// Auto-invite the service via the service factory
					if (DBG) System.out.println("Auto-requesting service for " + role);
					mJunctionMaker.inviteActorService(jx,role);
					// TODO: add a method that takes in a Junction
					// so we don't have to do an extra lookup
				}
			}
		}
	}
}
