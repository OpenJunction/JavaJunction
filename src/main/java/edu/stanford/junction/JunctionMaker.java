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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;


import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.Cast;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.provider.JunctionProvider;
import edu.stanford.junction.provider.jvm.JVMSwitchboardConfig;
import edu.stanford.junction.provider.jx.JXSwitchboardConfig;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;
import edu.stanford.junction.provider.irc.IRCSwitchboardConfig;


public class JunctionMaker {
	public static final String DIRECTOR_ACTIVITY = "edu.stanford.junction.director";
	public static final URI  CASTING_DIRECTOR;
	static {
		URI dumbWayToAssignStaticFinalURI;
		try {
			dumbWayToAssignStaticFinalURI = new URI("junction://jx-director-local/cast");
		} catch (Exception e ) {
			dumbWayToAssignStaticFinalURI = null;
		}
		CASTING_DIRECTOR = dumbWayToAssignStaticFinalURI;
	}
	protected JunctionProvider mProvider;
	
	public static JunctionMaker getInstance(SwitchboardConfig switchboardConfig) {
		// TODO: map config to maker?
		JunctionMaker maker = new JunctionMaker();
		maker.mProvider = maker.getProvider(switchboardConfig);
		maker.mProvider.setJunctionMaker(maker);
		return maker;
	}
	
	public static String getSessionIDFromURI(URI uri) {
		try {
			return uri.getPath().substring(1);
		} catch (Exception e) {
			return null;
		}
	}
	
	protected JunctionMaker() {
		
	}
	
	
	protected JunctionProvider getProvider(SwitchboardConfig switchboardConfig) {
		if (switchboardConfig instanceof XMPPSwitchboardConfig) {
			return new edu.stanford.junction.provider.xmpp.JunctionProvider((XMPPSwitchboardConfig)switchboardConfig);
		} else if (switchboardConfig instanceof JVMSwitchboardConfig){
			return new edu.stanford.junction.provider.jvm.JunctionProvider((JVMSwitchboardConfig)switchboardConfig);
		} else if (switchboardConfig instanceof JXSwitchboardConfig) {
			return new edu.stanford.junction.provider.jx.JunctionProvider((JXSwitchboardConfig)switchboardConfig);
		} else if (switchboardConfig instanceof IRCSwitchboardConfig) {
			return new edu.stanford.junction.provider.irc.JunctionProvider((IRCSwitchboardConfig)switchboardConfig);
		} else {
			// Unknown implementation;.
			return null;
		}
	}
	
	/*
	 * JunctionMaker has three functions:
	 * (1) Connect an Actor to a Junction
	 * (2) Retrieve an activity's script given a URI
	 * (3) Support various invitation mechanisms (often platform-specific)
	 */

	
	/**
	 * This method has been deprecated. Please see 
	 * {@link newJunction(URI, ActivityScript, JunctionActor)}
	 */
	@Deprecated
	public Junction newJunction(URI uri, JunctionActor actor) throws JunctionException {
		return mProvider.newJunction(uri, null, actor);
	}
	
	public Junction newJunction(URI uri, ActivityScript script, JunctionActor actor) throws JunctionException{
		return mProvider.newJunction(uri, script, actor);
	}
	
	public Junction newJunction(ActivityScript desc, JunctionActor actor) throws JunctionException{
		URI sessionUri;
		if (desc == null) {
			desc = new ActivityScript();
		}
		if (desc.getSessionID() == null) {
			sessionUri = mProvider.generateSessionUri();
			desc.isActivityCreator(true);
			desc.setUri(sessionUri);
		} else {
			// TODO: get rid of this.
			try {
				sessionUri = new URI("junction://" + desc.getHost() + "/" + desc.getSessionID());
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Bad URI from activity script");
			}
		}

		return mProvider.newJunction(sessionUri, desc, actor);
	}
	
	public URI generateSessionUri() {
		return mProvider.generateSessionUri();
	}
	
	/**
	 * Creates a new Junction and requests the casting of various roles.
	 * There is no guarantee if and when the roles will be filled.
	 * There may or may not be user interaction at the casting director.
	 * 
	 * @param desc
	 * @param actor
	 * @param support
	 * @return
	 */
	public Junction newJunction(ActivityScript desc, JunctionActor actor, Cast support) throws JunctionException{
		Junction jx = newJunction(desc, actor);
		//System.out.println("creating activity " + desc.getJSON());
		int size=support.size();
		System.out.println("going to cast " + size + " roles");
		for (int i=0;i<size;i++){
			if (support.getDirector(i) != null) {
				//System.out.println("Casting role " + support.getRole(i) + " on " + support.getDirector(i));
				URI invitationURI = jx.getInvitationURI(support.getRole(i));
				this.castActor(support.getDirector(i), invitationURI);
			}
		}
		
		return jx;
	}
	
	/**
	 * Sends a message to an activity. In the
	 * unoptimized case, joins an activity, sends
	 * a message, and leaves.
	 * 
	 * @param activitySession
	 * @param msg
	 */
	public void sendMessageToActivity(URI activitySession, JSONObject msg) throws JunctionException{
		mProvider.sendMessageToActivity(activitySession,msg);
	}
	
	
	public ActivityScript getActivityScript(URI uri) throws JunctionException{
		return mProvider.getActivityScript(uri);
	}
	
	/**
	 * Sends a request to a Director activity
	 * to cast an actor to accept a given invitation.
	 * 
	 * @param directorURI The director listening for requests
	 * @param invitationURI The activity to join (potentially including role information)
	 */
	public void castActor(final URI directorURI, final URI invitationURI) throws JunctionException{
		JunctionActor actor = new JunctionActor("inviter") {
				@Override
				public void onActivityJoin() {
					JSONObject invitation = new JSONObject();
					try {
						invitation.put("action","cast");
						invitation.put("activity", invitationURI.toString());
						getJunction().sendMessageToSession(invitation);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}

					leave();
				}
			
				@Override
				public void onMessageReceived(MessageHeader header,
											  JSONObject message) {
				
				}
			};
		
		JunctionMaker.this.newJunction(directorURI, actor);
	}
	
	
	/**
	 * Returns the role associated with a given Junction invitation.
	 * @param uri
	 * @return
	 */
	public static String getRoleFromInvitation(URI uri) {
		String query = uri.getQuery();
		if (query == null) return null;
		int pos = query.indexOf("role=");
		if (pos == -1) {
			return null;
		}

		query = query.substring(pos+5);
		pos = query.indexOf('&');
		if (pos > -1) {
			query = query.substring(0,pos);
		}
		return query;
	}
	
	@Deprecated
	public void inviteActorByListenerService(final URI invitationURI, URI listenerServiceURI) throws JunctionException {
		JunctionActor actor = new JunctionActor("inviter") {
				@Override
				public void onActivityJoin() {
					JSONObject invitation = new JSONObject();
					try {
						invitation.put("activity", invitationURI.toString());
						getJunction().sendMessageToSession(invitation);
					} 
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
					leave();
				}
			
				@Override
				public void onMessageReceived(MessageHeader header,
											  JSONObject message) {
				
				}
			};
		
		JunctionMaker.this.newJunction(listenerServiceURI, actor);
	}
	
	
	/**
	 * Requests a listening service to join this activity as the prescribed role. Here,
	 * the service must be detailed in the activity description's list of roles.
	 * 
	 * An example platform in the role specification:
	 * { role: "dealer", platforms: [ 
	 * 						{ platform: "jxservice", 
	 * 						  classname: "edu.stanford.prpl.poker.dealer",
	 * 						  switchboard: "my.foreign.switchboard" } ] }
	 * 
	 * If switchboard is not present, it is assumed to be on the same switchboard
	 * on which this activity is being run.
	 * 
	 * @param role
	 * @param host
	 * @param serviceName
	 */
	@Deprecated
	public void inviteActorService(final Junction jx, final String role)  throws JunctionException{
		ActivityScript desc = jx.getActivityScript();
		System.out.println("Desc: " + desc.getJSON().toString());
		// find service platform spec
		
		System.out.println("inviting service for role " + role);
			
		JSONObject platform = desc.getRolePlatform(role, "jxservice");
		System.out.println("got platform " + platform);
		if (platform == null) return;
			
		String switchboard = platform.optString("switchboard");
		System.out.println("switchboard: " + switchboard);
		if (switchboard == null || switchboard.length() == 0) {
			switchboard = jx.getSwitchboard();
			System.out.println("switchboard is null, new: " + switchboard);
		}
		final String serviceName = platform.optString("serviceName");
			
		// // // // // // // // // // // // // // // // 
		JunctionActor actor = new JunctionActor("inviter") {
				@Override
				public void onActivityJoin() {
					JSONObject invitation = new JSONObject();
					try {
						invitation.put("activity", jx.getInvitationURI(role));
						invitation.put("serviceName",serviceName);
					} catch (Exception e) {}
					getJunction().sendMessageToSession(invitation);
					leave();
				}
				
				@Override
				public void onMessageReceived(MessageHeader header,
											  JSONObject message) {
					
				}
			};
			
		// remote jxservice activity:
		URI remoteServiceActivity=null;
		try {
			remoteServiceActivity = new URI("junction://"+switchboard+"/jxservice");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Inviting service at uri " + remoteServiceActivity);
		JunctionMaker.this.newJunction(remoteServiceActivity, actor);
	}

	public static SwitchboardConfig getDefaultSwitchboardConfig(URI uri) {
		String fragment = uri.getFragment();
		if (fragment == null) {
			fragment = "xmpp";
		}
		fragment = fragment.toLowerCase();
		if (fragment.equals("jvm")) {
			return new edu.stanford.junction.provider.jvm.JVMSwitchboardConfig();
		} else if (fragment.equals("jx")) {
			return new edu.stanford.junction.provider.jx.JXSwitchboardConfig();
		} else if (fragment.equals("jx")) {
			return new edu.stanford.junction.provider.jx.JXSwitchboardConfig();
		} else if (fragment.equals("irc")) {
			return new edu.stanford.junction.provider.irc.IRCSwitchboardConfig();
		}

		// assume xmpp
		return new edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig(uri);
	}
}
