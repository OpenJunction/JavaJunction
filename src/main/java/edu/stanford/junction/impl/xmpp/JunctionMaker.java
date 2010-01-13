package edu.stanford.junction.impl.xmpp;

import java.net.URI;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;

public class JunctionMaker extends edu.stanford.junction.JunctionMaker {
	protected String mSwitchboard; // TODO: SwitchboardConfig
	
	public JunctionMaker() {
		super();
	}
	
	public JunctionMaker(String switchboard) {
		mSwitchboard=switchboard;
	}
	
	
	
	public Junction newJunction(URI uri, JunctionActor actor) {
		ActivityScript desc = new ActivityScript();
		desc.setHost(uri.getHost());
		if (uri.getPath() != null) { // TODO: check to make sure this works for URIs w/o path
			desc.setSessionID(uri.getPath().substring(1));
		}
		
		return newJunction(desc,actor);
	}
	
	public Junction newJunction(ActivityScript desc, JunctionActor actor) {
		
		// this needs to be made more formal
		if (null == desc.getHost() && mSwitchboard != null) {
			desc.setHost(mSwitchboard);
		}
		
		Junction jx = new edu.stanford.junction.impl.xmpp.Junction(desc);
		jx.registerActor(actor);
		
		if (desc.isActivityCreator()) {
			String[] roles = desc.getRoles();
			for (String role : roles) {
				JSONObject plat = desc.getRolePlatform(role, "jxservice");
				if (plat != null) {
					// Auto-invite the service via the service factory
					System.out.println("Auto-requesting service for " + role);
					inviteActorService(jx,role);
					// TODO: add a method that takes in a Junction
					// so we don't have to do an extra lookup
				}
			}
		}
		
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return jx;
	}
	
	public ActivityScript getActivityDescription(URI uri) {
		
		// TODO: Move the XMPPConnection into the JunctionMaker
		// (out of Junction)
		/*
		JunctionMaker jm = null;
		String host = uri.getHost();
		if (host.equals(mSwitchboard)) {
			jm = this;
		} else {
			jm = new JunctionMaker(host);
		}
		 */
		
		String host = uri.getHost();
		String sessionID = uri.getPath().substring(1);
		XMPPConnection conn = getXMPPConnection(host);
		
		String room = sessionID+"@conference."+host;
		System.err.println("looking up info from xmpp room " + room);
		
		
		try {
			RoomInfo info = MultiUserChat.getRoomInfo(conn, room);
			System.err.println("room desc " + info.getDescription());
			System.err.println("room subj " + info.getSubject());
			System.err.println("part " + info.getOccupantsCount());
			System.err.println("room " + info.getRoom());
			
			String descString = info.getDescription();
			if (descString == null || descString.trim().length()==0) {
				System.err.println("No MUC room description found.");
				return null;
			}
			
			JSONObject descJSON = new JSONObject(descString);
			
			return new ActivityScript(descJSON);
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	
	private XMPPConnection getXMPPConnection(String host) {
		XMPPConnection mXMPPConnection= new XMPPConnection(host);
		try {
			mXMPPConnection.connect();
			mXMPPConnection.loginAnonymously();
			
			return mXMPPConnection;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
