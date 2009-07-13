package edu.stanford.prpl.junction.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import edu.stanford.prpl.junction.api.activity.JunctionActor;


public class JunctionMaker {
	private URL mHostURL;
	private JunctionManager mManager;
	
	public static JunctionMaker getInstance(URL url) {
		// todo: singleton per-URL?
		return new JunctionMaker(url);
	}
	
	public static JunctionMaker getInstance() {
		return new JunctionMaker();
	}
	
	private JunctionMaker() {
		
	}
	
	private JunctionMaker(URL url) {
		mHostURL=url;
		
		/*
		 * This is temporary until we know what
		 * the pieces will look like.
		 * The relevant JunctionManager pieces
		 * should be merged into this class.
		 */
		Map<String,Object>params = new HashMap<String,Object>();
		params.put("host", mHostURL.toExternalForm());
		mManager = new JunctionManager(params);
		
	}
	
	// TODO: add 0-arg constructor for activities w/ given junction hosts
	// or allow static call for newJunction?
	
	public Junction newJunction(String friendlyName, JunctionActor actor) {
		return null;
	}
	
	public Junction newJunction(URL url, JunctionActor actor) {
		return null;
	}
	
	public Junction newJunction(Map<String,Object>desc, JunctionActor actor) {
		Junction activity = new Junction(mManager);
		activity.registerActor(actor);
		// creating an activity is an activity using a JunctionService.
		// Invite the JunctionMaker service to the session.
		// This service will be bundled with all Junction servers.
		//activity.requestService("JunctionMaker", mHostURL, "edu.stanford.prpl.junction.impl.JunctionMakerService");		
		return activity;
	}
}
