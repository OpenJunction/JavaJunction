package edu.stanford.prpl.junction.sample.sql;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class SQLActor {
	// JunctionManager extends/implements JunctionAPI
	
	
	public static void main(String[] argv) {
		try {
			System.out.println("Starting the database actor");
			Map<String,Object> activity = new HashMap<String,Object>();
			activity.put("host", "http://prpl.stanford.edu:8181/cometd/cometd");
			activity.put("role", "sql-server");
			activity.put("sessionID","sqlQuerySession");
			
			JunctionAPI jm = new JunctionManagerFactory().create(activity);
		
			jm.registerQueryHandler(
				new QueryHandler()
			);
			
			Object dud = new Object();
			synchronized(dud){
				dud.wait();
			}
			
		} catch (Exception e) {
			System.err.println("fail.");
			e.printStackTrace();
		}
	}
	
	
}