package edu.stanford.junction.test.multiconnect;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class Receiver extends JunctionActor {
	String mName;
	public Receiver(String name) {
		super("receiver");
		mName=name;
	}

	
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject inbound) {
		System.out.println(mName + " :: " + inbound);
		
		try {
			JSONObject msg = new JSONObject();
			msg.put("thanksFor", inbound.get("tic"));
			//header.getReplyTarget().sendMessage(msg);
			sendMessageToActor(header.getSender(),msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
