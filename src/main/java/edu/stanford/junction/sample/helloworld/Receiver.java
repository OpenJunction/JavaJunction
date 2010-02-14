package edu.stanford.junction.sample.helloworld;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class Receiver extends JunctionActor {
	public Receiver() {
		super("receiver");
	}

	
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject inbound) {
		System.out.println(this + " got: " + inbound);
		
		try {
			JSONObject msg = new JSONObject();
			msg.put("thanksFor", inbound);
			//header.getReplyTarget().sendMessage(msg);
			sendMessageToRole("sender",msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
