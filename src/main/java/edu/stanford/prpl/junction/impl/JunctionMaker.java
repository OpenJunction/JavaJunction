package edu.stanford.prpl.junction.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.prpl.junction.api.activity.ActivityDescription;
import edu.stanford.prpl.junction.api.activity.JunctionActor;


public class JunctionMaker {
	private String mSwitchboard;
	
	public static JunctionMaker getInstance(String switchboard) {
		// todo: singleton per-URL?
		return new JunctionMaker(switchboard);
	}
	
	public static JunctionMaker getInstance() {
		return new JunctionMaker();
	}
	
	protected JunctionMaker() {
		
	}
	
	protected JunctionMaker(String switchboard) {
		mSwitchboard=switchboard;
	}
	
	// TODO: add 0-arg constructor for activities w/ given junction hosts
	// or allow static call for newJunction?
	
	public Junction newJunction(String friendlyName, JunctionActor actor) {
		return null;
	}
	
	public Junction newJunction(URL url, JunctionActor actor) {
		ActivityDescription desc = new ActivityDescription();
		desc.setHost(url.getHost());
		if (url.getPath() != null) { // TODO: check to make sure this works for URIs w/o path
			desc.setSessionID(url.getPath().substring(1));
		}
		
		return newJunction(desc,actor);
		
		/*
		  
		ActivityDescription desc = new ActivityDescription();
		String query = url.getQuery();
		
		String tmp;
		if (null != (tmp = getURLParam(query,"sessionID"))) {
			desc.setSessionID(tmp);
		}
		
		desc.setHost(url.getHost());
		//desc.setActorID(actor.getActorID());
		
		return newJunction(desc,actor);
		
		*/
	}
	
	private String getURLParam(String query, String param) {
		int pos = query.indexOf(param+"=");
		if (pos < 0) return null;
		
		String val = query.substring(pos+1+param.length());
		pos = val.indexOf("&");
		if (pos > 0)
			val = val.substring(0,pos);
		
		return val;
	}
	
	public Junction newJunction(ActivityDescription desc, JunctionActor actor) {
		
		// this needs to be made more formal
		if (null == desc.getHost() && mSwitchboard != null) {
			desc.setHost(mSwitchboard);
		}
		
		Junction jx = new Junction(desc);
		jx.registerActor(actor);
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return jx;
	}
	
	/*
	public Junction newJunction(Map<String,Object>desc, JunctionActor actor) {
		if (desc.get("host") == null && mSwitchboard == null) {
			return null;
		}
		
		if (desc.get("host") == null) {
			desc.put("host", mSwitchboard);
		}
		
		ActivityDescription activityDesc = new ActivityDescription(desc);
		return newJunction(activityDesc,actor);
	}
	*/
	
	
	public ActivityDescription getActivityDescription(URI uri) {
		
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
		
		try {
			RoomInfo info = MultiUserChat.getRoomInfo(conn, room);
			String descString = info.getDescription();
			JSONObject descJSON = new JSONObject(descString);
			
			return new ActivityDescription(descJSON);
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (JSONException e) {
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
