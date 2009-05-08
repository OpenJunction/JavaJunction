package edu.stanford.prpl.junction.api.object;

public interface ObjectStreamFactory {
	public OutboundObjectStream getNewOutboundStream();
	public InboundObjectStream getNewInboundStream();
	
}
