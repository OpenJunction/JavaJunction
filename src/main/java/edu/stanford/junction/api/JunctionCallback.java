package edu.stanford.junction.api;

import edu.stanford.junction.api.object.InboundObjectStream;

public interface JunctionCallback {
	public void onObjectReceived(InboundObjectStream inbound);
	public void onTermination(boolean wasRemote);
}
