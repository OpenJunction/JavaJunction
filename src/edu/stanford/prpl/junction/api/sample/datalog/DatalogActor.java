package edu.stanford.prpl.junction.api.sample.datalog;

import java.net.URL;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;
import edu.stanford.prpl.junction.api.query.JunctionQueryHandler;
import edu.stanford.prpl.junction.impl.JunctionManager;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class DatalogActor {
	// JunctionManager extends/implements JunctionAPI
	
	
	public static void main(String[] argv) {
		try {
			JunctionAPI jm = new JunctionManagerFactory().create(new URL("http://prpl.stanford.edu/cometd/cometd"));
		
			jm.registerQueryHandler(
				new DatalogQueryHandler()
			);
			
			
			Thread.sleep(60*60*1000);
			
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