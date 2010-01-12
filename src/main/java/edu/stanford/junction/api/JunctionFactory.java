package edu.stanford.junction.api;

import java.net.URL;
import java.util.Map;

public interface JunctionFactory {
	// initialize
	// host, sessionID, clientID, capabilities, etc.
	// public JunctionAPI create(JunctionActivity activity);
	public JunctionAPI create(Map<String,Object> activity);
	public JunctionAPI create(URL url);
}
