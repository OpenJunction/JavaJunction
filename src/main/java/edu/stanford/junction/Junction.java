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


package edu.stanford.junction;

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.ExtrasDirector;

/**
 * This class is the glue between an application developer's actor
 * and the service provider's junction implementation.
 */
public abstract class Junction {
	protected JunctionActor mOwner;
	public static String NS_JX = "jx";
	
	/**
	 * Required constructors
	 */
	public Junction() {}
	
	
	/** 
	 * Activity Description
	 */
	
	public abstract ActivityScript getActivityScript();
	
	public abstract URI getAcceptedInvitation();
	
	//public String getActivityID();
	public abstract String getSessionID();
	
	// TODO: doesn't make sense for other switchboard implementations.
	// return Maker object instead?
	@Deprecated
	public abstract String getSwitchboard();
	//public String[] getRoles();
	
	/**
	 * Activity Lifecycle
	 */
	// TODO: move to constructor?
	protected void setActor(JunctionActor actor) {
		mOwner = actor;
		mOwner.setJunction(this);
		
		List<JunctionExtra> extras = actor.getInitialExtras();
		if (extras != null){
			for (int i=0;i<extras.size();i++) {
				registerExtra(extras.get(i));
			}
		}
	}
	
	//public abstract void start();
	public abstract void disconnect();
	// public abstract void onStart(); // or register handler?
	
	/** 
	 * Actor Management
	 */
	
	//public abstract void registerActor(JunctionActor actor);
	//public List<String> getActorsForRole(String role);
	// getActorsForHuman(String id); // some way of getting actor(s) associated with a person
	//public void onActorJoin(JunctionActor actor); // or do we want registerActorJoinHandler()
	public JunctionActor getActor() {
		return mOwner;
	}
	
	/**
	 * Actor Invitation
	 */
	public abstract URI getBaseInvitationURI();
	
	public final URI getInvitationURI() {
		return getInvitationURI(null);
	}
	
	public URI getInvitationURI(String role) {
		URI uri = getBaseInvitationURI();
		
		Map<String,String>params = new HashMap<String, String>();
		if (role != null) {
			params.put("role",role);
		}
		mExtrasDirector.updateInvitationParameters(params);
		
		StringBuffer queryBuf = new StringBuffer("?");
		Set<String>keys = params.keySet();
		for (String key : keys) {
			try{
				queryBuf.append(URLEncoder.encode(key,"UTF-8")
							+"="
							+URLEncoder.encode(params.get(key),"UTF-8")
							+"&");
			} catch (Exception e){}
		}
		String queryStr = queryBuf.substring(0, queryBuf.length()-1);
		
		try {
			uri = new URI(uri.toString()+queryStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return uri;
	}
	
	// there will also be device-specific methods, EG QR codes / contact list on Android
	
	/**
	 * Messaging
	 */
	
	// send
	
	public abstract void doSendMessageToRole(String role, JSONObject message);
	public final void sendMessageToRole(String role, JSONObject message) {
		if (mExtrasDirector.beforeSendMessageToRole(role, message)) {
			doSendMessageToRole(role, message);
		}
	}
	
	public abstract void doSendMessageToActor(String actorID, JSONObject message);
	public final void sendMessageToActor(String actorID, JSONObject message) {
		if (mExtrasDirector.beforeSendMessageToActor(actorID, message)) {
			doSendMessageToActor(actorID,message);
		}
	}
	
	public abstract void doSendMessageToSession(JSONObject message);
	public final void sendMessageToSession(JSONObject message) {
		if (mExtrasDirector.beforeSendMessageToSession( message)) {
			doSendMessageToSession(message);
		}
	}
	
	// receive
	public void triggerMessageReceived(MessageHeader header, JSONObject message) {
		if (mExtrasDirector.beforeOnMessageReceived(header,message)) {
			getActor().onMessageReceived(header, message);
			mExtrasDirector.afterOnMessageReceived(header,message);
		}
	}
	
	public void triggerActorJoin(boolean isCreator) {
		// Create
		if (isCreator) {
			if (!mExtrasDirector.beforeActivityCreate()) {
				disconnect();
				return;
			}
			getActor().onActivityCreate();
			mExtrasDirector.afterActivityCreate();
		}
		
		// Join
		if (!mExtrasDirector.beforeActivityJoin()) {
			disconnect();
			return;
		}
		getActor().onActivityJoin();
		mExtrasDirector.afterActivityJoin();
	}
	
	private ExtrasDirector mExtrasDirector = new ExtrasDirector();
	final public void registerExtra(JunctionExtra extra) {
		extra.setActor(getActor());
		mExtrasDirector.registerExtra(extra);
	}
}
