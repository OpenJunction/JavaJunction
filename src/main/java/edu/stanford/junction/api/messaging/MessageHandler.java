package edu.stanford.junction.api.messaging;

import java.util.List;

import org.json.JSONObject;


public abstract class MessageHandler {
	/**
	 * Stream-selecting filters;
	 * These filters allow Junction to route messages
	 * using the network's capabilities.
	 * 
	 * If we are sending to a fixed actor, try and establish a direct connection.
	 * If we are sending to a fixed role or multiple actors, we can
	 * potentially multicast.
	 * 
	 * These methods should return static results (list of actors does not change
	 * over the lifetime of the handler)
	 */
	public List<String> supportedRoles() { return null; } // default is to not filter on roles.
	public List<String> supportedActors() { return null; } // default is to not filter on actors.
	public List<String> supportedChannels() { return null; } // default is to not filter on channel.
	
	/**
	 * Message-level filter
	 * 
	 * These filters may be convenient for the end-user.
	 * May be useful to support RPC, for example
	 * 
	 */
	public boolean supportsMessage(JSONObject message) { return true; }
	// TODO: have a dynamic filter and a static one.
	// the static one defines a message template, and this template can be
	// shared (like an XML XSD document)
	
	/**
	 * Message handling
	 */
	public abstract void onMessageReceived(MessageHeader header, JSONObject message);
	
	/**
	 * How to add response capabilities?
	 * This should be bridged with Junction.sendMessageToXXX
	 * We might need a MessageTarget or something.
	 */
	// getMessageRecipients;
	// getMessageSender;
	// replyToSender; 
	// replyAll
}
