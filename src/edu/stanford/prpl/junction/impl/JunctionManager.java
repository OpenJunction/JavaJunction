package edu.stanford.prpl.junction.impl;

import java.net.URL;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionCallback;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;
import edu.stanford.prpl.junction.api.query.JunctionQueryHandler;

public class JunctionManager implements JunctionAPI  {

	/**
	 * Query API
	 */
	
	// Send
	
	public void query(JunctionQuery query, JunctionCallback callback) {
		// if query.source supports 
	}

	public void query(JunctionQuery query, String channelName) {
		// TODO Auto-generated method stub

	}

	public InboundObjectStream query(JunctionQuery query) {
		// TODO Auto-generated method stub
		return null;
	}

	// Respond
	
	public void registerQueryHandler(JunctionQueryHandler handler) {
		// TODO Auto-generated method stub
		
	}

}
