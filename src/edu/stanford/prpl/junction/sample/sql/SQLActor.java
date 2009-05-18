package edu.stanford.prpl.junction.sample.sql;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class SQLActor {
	// JunctionManager extends/implements JunctionAPI
	
	
	public static void main(String[] argv) {
		try {
			System.out.println("Starting the database actor");
			JSONObject activity = new JSONObject();
			activity.put("host", "http://prpl.stanford.edu/cometd/cometd");
			activity.put("role", "sql-server");
			activity.put("sessionID","querySession");
			
			JunctionAPI jm = new JunctionManagerFactory().create(activity);
		
			jm.registerQueryHandler(
				new QueryHandler()
			);
			
			
			Thread.sleep(1*60*1000);
			
		} catch (Exception e) {
			System.err.println("fail.");
			e.printStackTrace();
		}
	}
	
	
}

/*
 
-- OR: --

class DatalogActor extends Junction.Actor {
	public List<JunctionQueryHandler> getQueryHandlers() { ...
}

JunctionManager.registerActor(DatalogActor.getInstance());


abstract class Junction.Actor {
	public JSONObject getDescriptor();
	// queries: use QueryHandler.supportID() to populate


	public Junction.Actor getInstance();
}

*/