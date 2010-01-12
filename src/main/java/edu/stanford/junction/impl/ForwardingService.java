package edu.stanford.junction.impl;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionService;
import edu.stanford.junction.api.messaging.JunctionMessage;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * This class allows a remote client to subscribe to a channel without having
 * to open a new connection for it. It may be useful for a singleton service 
 * expecting to connect to many activities.
 * @author bdodson
 *
 */
public class ForwardingService extends JunctionService {
	private String mChannel; // TODO: this needs to be on another server
	
	private ForwardingService() {}
	public static JunctionService newInstance() {
		return new ForwardingService();
	}
	
	@Override
	public String getServiceName() {
		return "JunctionMaker";
	}

	@Override
	public void onActivityStart() {
	}
	
	@Override
	public void onActivityJoin() {
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// TODO: get a new Junction for remote server
		// Figure out how to preserve sender info
		// ('originator' field or something?)
		//getJunction().sendMessageToChannel(mChannel, message)		
	}
	
}
