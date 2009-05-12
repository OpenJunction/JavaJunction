package edu.stanford.prpl.junction.sample.datalog;

import java.net.URL;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionCallback;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class DatalogActorQuerier {
	public static void main(String[] argv) {
		try {
			System.out.println("Starting the querier actor");
			JunctionAPI jm = new JunctionManagerFactory().create(new URL("http://prpl.stanford.edu/cometd/cometd"));
			
			DatalogQuery query = new DatalogQuery("SELECT name FROM jz_nodes WHERE ptype='genre'");
			JunctionCallback callback = new DatalogCallback();
			
			System.out.println("Sending query: " + query.getQueryText());
			jm.query(jm.channelForRole(DatalogConstants.actor.DATALOG_SERVER), query, callback);
			
			Thread.sleep(10*1000);
			callback.terminate();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
