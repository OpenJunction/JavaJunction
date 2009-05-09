package edu.stanford.prpl.junction.api;

import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;

public abstract class JunctionCallback {
	protected JunctionQuery mQuery;
	protected InboundObjectStream mStream;
	
	public abstract void onMessageReceived(InboundObjectStream stream);

	public void terminate() {
		if (mStream != null) {
			mStream.close();
		}
	}
	
	public JunctionQuery getQuery() {
		return mQuery;
	}
	
	protected void setQuery(JunctionQuery query) {
		mQuery=query;
	}
}
