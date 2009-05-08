package edu.stanford.prpl.junction.api.query;

import edu.stanford.prpl.junction.api.object.OutboundObjectStream;

public abstract class JunctionQueryHandler {
	public boolean supportsQuery(JunctionQuery query) {
		return true;
	}
	
	public abstract void handleQuery(JunctionQuery query, OutboundObjectStream result);
}
