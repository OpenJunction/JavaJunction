package edu.stanford.prpl.junction.impl;

import java.net.URL;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionFactory;

public class JunctionManagerFactory implements JunctionFactory {

	public JunctionAPI create(JSONObject activity) {
		// TODO Auto-generated method stub
		return new JunctionManager();
	}

	public JunctionAPI create(URL url) {
		// TODO Auto-generated method stub
		return new JunctionManager();
	}

}
