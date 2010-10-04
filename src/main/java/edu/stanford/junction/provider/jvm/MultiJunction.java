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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class MultiJunction {
	private static Map<URI,MultiJunction>multiJunctions = 
		Collections.synchronizedMap(
			new HashMap<URI,MultiJunction>());
	
	Map<String,JunctionActor> actorMap;
	URI uri;
	
	public static MultiJunction get(URI uri, JunctionActor actor) {
		MultiJunction j;
		if (!multiJunctions.containsKey(uri)) {
			j = new MultiJunction(uri);
			multiJunctions.put(uri,j);
		} else {
			j = multiJunctions.get(uri);
		}
		j.registerActor(actor);
		return j;
	}
	
	private MultiJunction(URI uri) {
		actorMap = new HashMap<String, JunctionActor>();
		this.uri = uri;
	}
	
	private void registerActor(JunctionActor actor) {
		actorMap.put(actor.getActorID(),actor);
	}
	
	protected void sendMessageToActor(JunctionActor from, String actorID, JSONObject message) {
		if (actorMap.containsKey(actorID)) {
			actorMap.get(actorID).getJunction().triggerMessageReceived(
					new MessageHeader(from.getJunction(),message,from.getActorID()),
					message);
		}
	}
	
	protected void sendMessageToRole(JunctionActor from, String role, JSONObject message) {
		if (role == null) return;
		for (JunctionActor actor : actorMap.values()) {
			for (String r : actor.getRoles()) {
				if (role.equals(r)) {
					actorMap.get(actor.getActorID()).getJunction().triggerMessageReceived(
							new MessageHeader(from.getJunction(),message,from.getActorID()),
							message);
				}
			}
		}
	}
	
	protected void sendMessageToSession(JunctionActor from, JSONObject message) {
		for (JunctionActor actor : actorMap.values()) {
			actorMap.get(actor.getActorID()).getJunction().triggerMessageReceived(
						new MessageHeader(from.getJunction(),message,from.getActorID()),
						message);
		}
	}
}
