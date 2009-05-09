package edu.stanford.prpl.junction.api.query;

import edu.stanford.prpl.junction.api.object.OutboundObjectStream;

public abstract class JunctionQueryHandler {
	public boolean supportsQuery(JunctionQuery query) {
		return true;
	}
	
	/*
	 * supportsQuery(JunctionQuery query, JunctionEnvironment env)
	 */
	
	
	public abstract void handleQuery(JunctionQuery query, OutboundObjectStream result /*, JunctionEvent event */);
}
