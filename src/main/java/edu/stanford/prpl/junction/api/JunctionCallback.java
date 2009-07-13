package edu.stanford.prpl.junction.api;

import edu.stanford.prpl.junction.api.object.InboundObjectStream;

public interface JunctionCallback {
	public void onObjectReceived(InboundObjectStream inbound);
	public void onTermination(boolean wasRemote);
}
