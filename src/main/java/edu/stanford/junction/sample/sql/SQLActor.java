package edu.stanford.junction.sample.sql;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.impl.xmpp.XMPPSwitchboardConfig;

public class SQLActor extends JunctionActor {
	// JunctionManager extends/implements JunctionAPI

	public SQLActor() {
		super("database");
	}
	
	public static void main(String[] argv) {
		try {
			System.out.println("Starting the database actor");
			JSONObject activity = new JSONObject();
			activity.put("sessionID", "sqlQuerySession");
			activity.put("ad","edu.stanford.prpl.junction.demo.sql");
			
			XMPPSwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			JunctionMaker jm = JunctionMaker.getInstance(config);
			Junction jx = jm.newJunction(new ActivityScript(activity), new SQLActor());

			/*
			try {
				Thread.sleep(3000);
				JSONObject msg = new JSONObject();
				msg.put("query","select count(*) from jz_nodes");
				jx.sendMessageToSession(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			
			Object dud = new Object();
			synchronized(dud){
				dud.wait();
			}
			
		} catch (Exception e) {
			System.err.println("fail.");
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityStart() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	static MessageHandler handler = null;
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// hack to fix from updating to onMessageReceived vs getMessageHandler
		if (handler == null) handler = new QueryHandler(this);
		
		handler.onMessageReceived(header, message);
	}	
}