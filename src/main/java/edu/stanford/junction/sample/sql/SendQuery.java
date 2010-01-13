package edu.stanford.junction.sample.sql;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;


import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

public class SendQuery extends JunctionActor {
	
	public SendQuery() {
		super("sql-client");
	}
	
	public static void main(String[] argv) {
		try {
			// todo: same session for client/server
			System.out.println("Starting the query actor");
			
			ActivityScript activity = new ActivityScript();
			
			Junction jx = JunctionMaker.getInstance().newJunction(activity, new SendQuery());
			
			
			
			//Thread.sleep(5000);
			//callback.terminate();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityStart() {
		
		//System.out.println("Sending query: " + query.getQueryText());
	}

	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// TODO Auto-generated method stub
		
	}
}
