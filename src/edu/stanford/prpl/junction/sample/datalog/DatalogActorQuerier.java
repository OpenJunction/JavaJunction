package edu.stanford.prpl.junction.sample.datalog;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.impl.JunctionCallback;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class DatalogActorQuerier {
	public static void main(String[] argv) {
		try {
			// todo: same session for client/server
			System.out.println("Starting the querier actor");
			JunctionAPI jm = new JunctionManagerFactory().create(new URL("http://prpl.stanford.edu/cometd/cometd"));
			
			JunctionQuery query = new JunctionQuery("Datalog","SELECT name FROM jz_nodes WHERE ptype='genre'");
			Map<String,Object>params = new HashMap<String,Object>();
			params.put("arg1","first arg");
			params.put("arg2",1003);
			query.setParameterMap(params);
			JunctionCallback callback = new DatalogCallback();
			
			System.out.println("Sending query: " + query.getQueryText());
			//jm.query(jm.channelForRole(DatalogConstants.actor.DATALOG_SERVER), query, callback);
			jm.query(jm.channelForSession(), query, callback);
			
			
			Thread.sleep(20*1000);
			callback.terminate();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
