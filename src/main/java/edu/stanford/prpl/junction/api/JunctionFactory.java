package edu.stanford.prpl.junction.api;

import java.net.URL;
import org.json.JSONObject;

public interface JunctionFactory {
	// initialize
	// host, sessionID, clientID, capabilities, etc.
	// public JunctionAPI create(JunctionActivity activity);
	public JunctionAPI create(JSONObject activity);
	public JunctionAPI create(URL url);
}
