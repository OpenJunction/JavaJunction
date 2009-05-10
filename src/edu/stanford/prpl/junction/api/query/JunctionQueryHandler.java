package edu.stanford.prpl.junction.api.query;

import java.util.List;

import edu.stanford.prpl.junction.api.object.OutboundObjectStream;

public abstract class JunctionQueryHandler {
	public boolean supportsQuery(JunctionQuery query) {
		return true;
	}
	
	public List<String> acceptedChannels() {
		return null; // means any access method: client-direct, role, session.
	}
	
	/*
	 * supportsQuery(JunctionQuery query, JunctionEnvironment env)
	 */
	
	
	public abstract void handleQuery(JunctionQuery query, OutboundObjectStream result /*, JunctionEvent event */);
}
