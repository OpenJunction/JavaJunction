package edu.stanford.prpl.junction.impl;

import org.cometd.Client;
import org.cometd.Message;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;

public class JunctionMakerService extends JunctionService {
	
	private JunctionMakerService() {}
	public static JunctionService newInstance() {
		return new JunctionMakerService();
	}
	
	@Override
	public String getServiceName() {
		return "JunctionMaker";
	}

	@Override
	public void onActivityStart() {
		System.out.println("JunctionMaker: activity has started!");
	}
	
	@Override
	public void onActivityJoin(Junction activity) {
		System.out.println("JunctionMaker joined activity w/ session id " + activity.getSessionID());
		activity.start();
	}

	@Override
	public MessageHandler getMessageHandler() {
		return new MessageHandler() {
			public void onMessageReceived(Client from, Message message) {
				System.out.println("maker got message: " + message);
			}
		};
	}
	
}
