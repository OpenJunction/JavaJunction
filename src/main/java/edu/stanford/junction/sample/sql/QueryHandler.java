/*
 * Copyright (C) 2010 Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package edu.stanford.junction.sample.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONException;
import org.json.JSONObject;


import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHandler;
import edu.stanford.junction.api.messaging.MessageHeader;

public class QueryHandler extends MessageHandler {

	JunctionActor mActor;
	
	public QueryHandler(JunctionActor actor) {
		mActor=actor;
	}
	
	public static void main(String[] argv) throws JSONException {
		//JunctionQuery q = new JunctionQuery("sql","SELECT name,playcount FROM jz_nodes WHERE ptype='genre'");
		//new QueryHandler(null).onMessageReceived(null,q);
		
	}
	
	
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
				
		//String query = q.getQueryText();
		String query = message.optString("query");
		
		query = query.toLowerCase();
		
		if (!query.contains("select")) return;
		if (query.contains("drop") || query.contains("delete")) return;
		System.out.println("Got query: " + query);
		
		Connection connection = null;
	    try {
	        // Load the JDBC driver
	        String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
	        Class.forName(driverName);
	    
	        // Create a connection to the database
	        //String serverName = "192.168.1.122";
	        String serverName = "127.0.0.1";
	        String mydatabase = "jinzora3";
	        String url = "jdbc:mysql://" + serverName +  "/" + mydatabase; // a JDBC url
	        String username = "jinzora";
	        String password = "jinzora";
	        connection = DriverManager.getConnection(url, username, password);
	    } catch (ClassNotFoundException e) {
	        // Could not find the database driver
	    	e.printStackTrace();
	    } catch (SQLException e) {
	        // Could not connect to the database
	    	e.printStackTrace();
	    }

	    try {
	    	Statement stmt = connection.createStatement();
	    	ResultSet rs = stmt.executeQuery(query);
	    	
	        ResultSetMetaData rsMetaData = rs.getMetaData();
	        int cols = rsMetaData.getColumnCount();
	        
	    	while (rs.next()) {
	    		
	    		JSONObject row = new JSONObject();
	    		try {
		    		for (int i = 1; i <= cols; i++) { // stupid indexing
		    			row.put(rsMetaData.getColumnName(i), rs.getObject(i));
		    		}
	    		} catch (JSONException e) {
	    			e.printStackTrace();
	    		}
	    		System.out.println("sending " + row);
	    		if (mActor != null) {
	    			//mActor.getJunction().sendMessageToTarget(header.getReplyTarget(),row);
	    			header.getReplyTarget().sendMessage(row);
	    		}
	    	}
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
		
		
		System.out.println("closing stream.");
		//results.close();
	  }
}
