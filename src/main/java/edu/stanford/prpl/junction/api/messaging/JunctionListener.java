package edu.stanford.prpl.junction.api.messaging;

import org.cometd.Client;
import org.cometd.Message;

public interface JunctionListener {
	public void onMessageReceived(Client from, Message message);
	
}