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


package edu.stanford.junction.provider.irc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * Implements Junction transport using a simple socket interface.
 *
 */
public class JunctionProvider extends edu.stanford.junction.provider.JunctionProvider {

	
	
	public JunctionProvider(IRCSwitchboardConfig config) {}
	


	@Override
	public ActivityScript getActivityScript(URI uri) {
		JunctionActor actor = new JunctionActor("scriptpuller") {
			public void onMessageReceived(MessageHeader header, JSONObject message) {}
		};
		Junction jx = new edu.stanford.junction.provider.irc.Junction(uri, null, actor);
		ActivityScript script = jx.getActivityScript();
		actor.leave();
		return script;
	}

	@Override
	public Junction newJunction(URI uri, ActivityScript script, JunctionActor actor) {
		return new edu.stanford.junction.provider.irc.Junction(uri,script,actor);
	}

	@Override
	public URI generateSessionUri() {
		try {
			String sessionName = UUID.randomUUID().toString();
			return new URI("junction://sb/" + sessionName + "#irc");
		} catch (URISyntaxException e) {
			throw new AssertionError("Invalid URI");
		}
	}
}
