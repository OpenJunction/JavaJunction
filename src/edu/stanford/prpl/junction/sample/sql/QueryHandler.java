package edu.stanford.prpl.junction.sample.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.api.messaging.JunctionQueryHandler;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;

public class QueryHandler extends JunctionQueryHandler {

	public static void main(String[] argv) {
		JunctionQuery q = new JunctionQuery("sql","SELECT name FROM jz_nodes WHERE ptype='genre'");
		new QueryHandler().handleQuery(q,null);
		
	}
	
	@Override
	public boolean supportsQuery(JunctionQuery query) {
		if (!query.getQueryType().equals("sql")) return false;
		
		if (query.getQueryText().toLowerCase().contains("drop")) return false;
		if (query.getQueryText().toLowerCase().contains("delete")) return false;
		if (query.getQueryText().toLowerCase().contains("insert")) return false;
		
		return true;
	}
	
	
	@Override
	public void handleQuery(JunctionQuery q, OutboundObjectStream results) {
		
		String query = q.getQueryText();
		
		System.out.println("Got query: " + query);

		Connection connection = null;
	    try {
	        // Load the JDBC driver
	        String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
	        Class.forName(driverName);
	    
	        // Create a connection to the database
	        String serverName = "192.168.1.122";
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
	    	
	    	while (rs.next()) {
	    		System.out.println("got " + rs.getString(1));
	    		results.send(rs.getString(1));
	    	}
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
		
		
		
		results.close();
	  }
}
