package edu.stanford.prpl.junction.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jivesoftware.smack.XMPPConnection;

import edu.stanford.prpl.junction.api.activity.ActivityDescription;
import edu.stanford.prpl.junction.api.activity.JunctionActor;


public class JunctionMaker {
	private URL mHostURL;
	
	public static JunctionMaker getInstance(URL url) {
		// todo: singleton per-URL?
		return new JunctionMaker(url);
	}
	
	public static JunctionMaker getInstance() {
		return new JunctionMaker();
	}
	
	protected JunctionMaker() {
		
	}
	
	protected JunctionMaker(URL url) {
		mHostURL=url;
	}
	
	// TODO: add 0-arg constructor for activities w/ given junction hosts
	// or allow static call for newJunction?
	
	public Junction newJunction(String friendlyName, JunctionActor actor) {
		return null;
	}
	
	public Junction newJunction(URL url, JunctionActor actor) {
		ActivityDescription desc = new ActivityDescription();
		String query = url.getQuery();
		
		String tmp;
		if (null != (tmp = getURLParam(query,"sessionID"))) {
			desc.setSessionID(tmp);
		}
		
		desc.setHost(url.getHost());
		desc.setActorID(actor.getActorID());
		
		return newJunction(desc,actor);
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
		Junction jx = new Junction(desc);
		jx.registerActor(actor);
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return jx;
	}
	
	public Junction newJunction(Map<String,Object>desc, JunctionActor actor) {
		if (desc.get("host") == null && mHostURL == null) {
			return null;
		}
		
		if (desc.get("host") == null) {
			desc.put("host", mHostURL.toExternalForm());
		}
		
		ActivityDescription activityDesc = new ActivityDescription(desc);
		return newJunction(activityDesc,actor);
	}
}
