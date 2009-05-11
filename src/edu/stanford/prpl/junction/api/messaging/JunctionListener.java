package edu.stanford.prpl.junction.api.messaging;

import org.cometd.Client;

public interface JunctionListener {
	public void onMessageReceived(Client from, Object data);
	
}