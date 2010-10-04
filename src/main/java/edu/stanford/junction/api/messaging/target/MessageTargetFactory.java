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


package edu.stanford.junction.api.messaging.target;

import edu.stanford.junction.Junction;

public class MessageTargetFactory {
	private Junction jx;
	
	public static MessageTargetFactory getInstance(Junction jx) {
		return new MessageTargetFactory(jx);
	}
	
	private MessageTargetFactory(Junction jx) {
		this.jx=jx;
	}
	
	public MessageTarget getTarget(String target) {
		if (target.equals("session")) {
			return new Session(jx);
		}
		
		if (target.startsWith("actor:")) {
			return new Actor(jx,target.substring(6));
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
}
