package edu.stanford.prpl.junction.impl;

import java.net.URL;
import java.util.List;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionCallback;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;
import edu.stanford.prpl.junction.api.query.JunctionQueryHandler;

public class JunctionManager implements JunctionAPI  {
	private JSONObject mDescriptor;
	
	/**
	 * Constructor
	 */
	public JunctionManager(JSONObject desc) {
		mDescriptor=desc;
	}
	
	/**
	 * Session Management
	 */
	public JSONObject getActivityDescriptor() {
		return mDescriptor;
	}
	
	
	/**
	 * Channel reference API
	 */
	public String channelForRole(String role) {
		return "/role/"+role;
	}
	
	public String channelForSession() {
		return "/session/generate_me";
	}
	
	
	/**
	 * Query API
	 */
	
	// Send
	
	public void query(String target, JunctionQuery query, JunctionCallback callback) {
		// if query.source supports 
	}

	public void query(String target, JunctionQuery query, String channelName) {
		// TODO Auto-generated method stub

	}

	public InboundObjectStream query(String target, JunctionQuery query) {
		// TODO Auto-generated method stub
		return null;
	}

	// Respond
	
	public void registerQueryHandler(JunctionQueryHandler handler) {
		List<String> channels = handler.acceptedChannels();
		if (null == channels) {
			String chan = channelForSession();
			
			
		} else {
			for (String chan : channels) {
				
			}
		}
	}

}
