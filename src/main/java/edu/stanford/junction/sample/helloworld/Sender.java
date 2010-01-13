package edu.stanford.junction.sample.helloworld;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class Sender extends JunctionActor {
	public Sender() {
		super("sender");
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		System.out.println("sender got " + message);
	}
	
	@Override
	public void onActivityJoin() {
		//new Thread() {
		//	public void run() {
				try {
					int tic = 0;
					JSONObject msg = new JSONObject();
					while (true) {
						msg.put("tic", tic++);
						sendMessageToRole("receiver", msg);
						Thread.sleep(5000);
					} 
				} catch (Exception e) {}
		//	}
		//}.start();
	}
}