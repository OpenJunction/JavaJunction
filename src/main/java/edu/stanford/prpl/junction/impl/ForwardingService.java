package edu.stanford.prpl.junction.impl;

import org.cometd.Client;
import org.cometd.Message;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;

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
	public MessageHandler getMessageHandler() {
		return new MessageHandler() {
			public void onMessageReceived(Client from, Message message) {
				// TODO: get a new Junction for remote server
				// Figure out how to preserve sender info
				// ('originator' field or something?)
				getJunction().sendMessageToChannel(mChannel, message);
			}
		};
	}
	
}