package edu.stanford.prpl.junction.sample.sql;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.impl.JunctionCallback;
import edu.stanford.prpl.junction.impl.JunctionManagerFactory;

public class SendQuery {
	public static void main(String[] argv) {
		try {
			// todo: same session for client/server
			System.out.println("Starting the query actor");
			
			JSONObject activity = new JSONObject();
			activity.put("host", "http://prpl.stanford.edu:8181/cometd/cometd");
			activity.put("role", "sql-client");
			activity.put("sessionID","sqlQuerySession");
			JunctionAPI jm = new JunctionManagerFactory().create(activity);
			
			JunctionQuery query = new JunctionQuery("sql","SELECT name FROM jz_nodes WHERE ptype='genre'");
			
			System.out.println("Sending query: " + query.getQueryText());
			
			JunctionCallback callback =  new JunctionCallback() {
				@Override
				public void onObjectReceived(InboundObjectStream stream) {
					try {
						while (stream.hasObject()) {
							Object o = stream.receive();
							System.out.println("result: " + o);
						}
					} catch (IOException e) {
						
					}
				}
			};
			
			jm.query(jm.channelForSession(), query, callback);
			
			
			Thread.sleep(5*1000);
			callback.terminate();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
