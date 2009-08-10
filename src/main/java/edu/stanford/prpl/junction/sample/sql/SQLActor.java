package edu.stanford.prpl.junction.sample.sql;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.activity.Junction;
import edu.stanford.prpl.junction.api.activity.JunctionActor;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.impl.JunctionMaker;

public class SQLActor extends JunctionActor {
	// JunctionManager extends/implements JunctionAPI

	public SQLActor() {
		super("database");
	}
	
	public static void main(String[] argv) {
		try {
			System.out.println("Starting the database actor");
			Map<String,Object> activity = new HashMap<String,Object>();
			activity.put("host", "prpl.stanford.edu");
			activity.put("role", "sql-server");
			activity.put("sessionID","sqlQuerySession");
			//activity.put("owner",true);
			
			JunctionMaker jm = JunctionMaker.getInstance();
			Junction jx = jm.newJunction(activity, new SQLActor());

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
	
	@Override
	public MessageHandler getMessageHandler() {
		return new QueryHandler(this);
	}
	
	
}