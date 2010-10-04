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


package edu.stanford.junction.api.messaging;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.messaging.target.MessageTarget;
import edu.stanford.junction.api.messaging.target.MessageTargetFactory;

public class MessageHeader {
	private Junction jx;
	private JSONObject message;
	public String from;
	
	public MessageHeader(Junction jx, JSONObject message, String from) {
		this.jx=jx;
		this.message=message;
		this.from=from;
	}
	
	
	public MessageTarget getReplyTarget() {
		if (message.has(Junction.NS_JX)) {
			JSONObject h = message.optJSONObject(Junction.NS_JX);
			if (h.has("replyTo")) {
				return MessageTargetFactory.getInstance(jx).getTarget(h.optString("replyTo"));
			}
		}
		
		return MessageTargetFactory.getInstance(jx).getTarget("actor:"+from);
	}
	
	public String getSender() {
		return from;
	}
	
	public Junction getJunction() { return jx; }
}
