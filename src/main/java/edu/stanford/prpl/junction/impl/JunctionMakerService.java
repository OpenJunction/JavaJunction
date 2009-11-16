package edu.stanford.prpl.junction.impl;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.activity.JunctionService;
import edu.stanford.prpl.junction.api.messaging.JunctionMessage;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.api.messaging.MessageHeader;

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
	public void onActivityJoin() {
		System.out.println("JunctionMaker joined activity w/ session id " + getJunction().getSessionID());
		getJunction().start();
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		System.out.println("maker got message: " + message);
	}
	
}
