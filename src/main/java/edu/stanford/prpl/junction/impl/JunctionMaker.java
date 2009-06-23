package edu.stanford.prpl.junction.impl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import edu.stanford.prpl.junction.api.activity.JunctionActivity;
import edu.stanford.prpl.junction.api.activity.JunctionRole;

public class JunctionMaker {
	private URL mHostURL;
	private JunctionManager mManager;
	
	public JunctionMaker(URL url) {
		mHostURL=url;
		
		/*
		 * This is temporary until we know what
		 * the pieces will look like.
		 * The relevant JunctionManager pieces
		 * should be merged into this class.
		 */
		Map<String,Object>params = new HashMap<String,Object>();
		params.put("host", url.toExternalForm());
		mManager = new JunctionManager(params);
		
	}
	
	
	public JunctionActivity newActivity(String friendlyName) {
		JunctionActivity activity = new JunctionActivity();
		
		// creating an activity is an activity.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with the Junction server.
		
		
		return activity;
	}
	
	public JunctionActivity newActivity(URL url) {
		return null;
	}
	
	public JunctionActivity newActivity(Map<String,Object>activity) {
		return null;
	}
}
