package edu.stanford.junction.test.multiconnect;

import java.net.URI;

import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class Sender extends JunctionActor {
	String mName;
	public Sender(String n) {
		super("sender");
		
		mName=n;
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		System.out.println(mName + " :: " + message);
	}
	
	@Override
	public void onActivityJoin() {
		//new Thread() {
		//	public void run() {
				try {
					int tic = 0;
					JSONObject msg = new JSONObject();
					while (tic<5) {
						msg.put("tic", tic++);
						sendMessageToRole("receiver", msg);
						Thread.sleep(3000);
					} 
				} catch (Exception e) {
					e.printStackTrace();
				}
		//	}
		//}.start();
	}
}