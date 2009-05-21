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