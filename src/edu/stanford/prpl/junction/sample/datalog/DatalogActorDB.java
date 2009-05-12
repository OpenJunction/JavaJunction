package edu.stanford.prpl.junction.sample.datalog;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class DatalogActorDB {
	// JunctionManager extends/implements JunctionAPI
	
	
	public static void main(String[] argv) {
		try {
			System.out.println("Starting the database actor");
			JSONObject role = new JSONObject();
			role.put("host", "http://prpl.stanford.edu/cometd/cometd");
			role.put("role", DatalogConstants.actor.DATALOG_SERVER);
			
			JunctionAPI jm = new JunctionManagerFactory().create(role);
		
			jm.registerQueryHandler(
				new DatalogQueryHandler()
			);
			
			
			Thread.sleep(10*60*1000);
			
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