package edu.stanford.prpl.junction.sample.sql;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;


import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.activity.ActivityDescription;
import edu.stanford.prpl.junction.api.activity.Junction;
import edu.stanford.prpl.junction.api.activity.JunctionActor;
import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.impl.JunctionMaker;

public class SendQuery extends JunctionActor {
	
	public SendQuery() {
		super("sql-client");
	}
	
	public static void main(String[] argv) {
		try {
			// todo: same session for client/server
			System.out.println("Starting the query actor");
			
			ActivityDescription activity = new ActivityDescription();
			
			Junction jx = JunctionMaker.getInstance().newJunction(activity, new SendQuery());
			
			
			
			//Thread.sleep(5000);
			//callback.terminate();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityStart() {
		JunctionQuery query = null;
		try {
			query = new JunctionQuery("sql","SELECT name FROM jz_nodes WHERE ptype='genre'");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("Sending query: " + query.getQueryText());
	}
}
