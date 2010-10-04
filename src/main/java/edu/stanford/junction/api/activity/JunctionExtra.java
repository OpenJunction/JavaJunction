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


package edu.stanford.junction.api.activity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public abstract class JunctionExtra {
	JunctionActor mParent=null;
	
	/**
	 * Provides access to the associated JunctionActor.
	 * @return
	 */
	public final JunctionActor getActor() {
		return mParent;
	}
	
	/**
	 * This method should only be called internally.
	 * @param actor
	 */
	public void setActor(JunctionActor actor) {
		mParent=actor;
	}
	
	/**
	 * Update the parameters that will be sent in an invitation
	 */
	public void updateInvitationParameters(Map<String,String>params) {
		
	}
	
	/**
	 * Returns true if the normal event handling should proceed;
	 * Return false to stop cascading.
	 */
	public boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) { return true; }
	public void afterOnMessageReceived(MessageHeader h, JSONObject msg) {}
	
	
	public boolean beforeSendMessageToActor(String actorID, JSONObject msg) { return beforeSendMessage(msg); }
	public boolean beforeSendMessageToRole(String role, JSONObject msg) { return beforeSendMessage(msg); }
	public boolean beforeSendMessageToSession(JSONObject msg) { return beforeSendMessage(msg); }
	
	/**
	 * Convenience method to which, by default, all message sending methods call through.
	 * @param msg
	 * @return
	 */
	public boolean beforeSendMessage(JSONObject msg) { return true; }
	//public boolean afterSendMessage(Header h, Message msg) {}
	
	/**
	 * Called before an actor joins an activity.
	 * Returning false aborts the attempted join.
	 */
	public boolean beforeActivityJoin() {
		return true;
	}
	
	public void afterActivityJoin() {
		
	}
	
	/**
	 * Called before an actor joins an activity.
	 * Returning false aborts the attempted join.
	 */
	public boolean beforeActivityCreate() {
		return true;
	}
	
	public void afterActivityCreate() {
		
	}

	
	//public void beforeGetActivityScript();
	
	/**
	 * Returns an integer priority for this Extra.
	 * Lower priority means closer to switchboard;
	 * Higher means closer to actor.
	 */
	public Integer getPriority() { return 20; }
	
	public void test() {
		JunctionActor actor =
			new JunctionActor("unittest") {			
				@Override
				public void onMessageReceived(MessageHeader header, JSONObject message) {
					System.out.println(message.toString());
				}
				
				@Override
				public List<JunctionExtra> getInitialExtras() {
					List<JunctionExtra> extras = new ArrayList<JunctionExtra>();
					extras.add(JunctionExtra.this);
					return extras;
				}
			};
			
		try {
			// todo: registeredextras here
			SwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			JunctionMaker.getInstance(config).newJunction(new URI("junction://prpl.stanford.edu/junit-test"), actor);
			synchronized(this) {
				this.wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
