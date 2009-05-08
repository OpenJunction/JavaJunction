package edu.stanford.prpl.junction.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.api.object.ObjectStreamFactory;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;


/**
 * A factory to create inbound and outbound object streams.
 * @author Matthew Nasielski
 * @author Ben Dodson
 *
 */
public class XStreamObjectStreamFactory implements ObjectStreamFactory
{
	public OutboundObjectStream getNewOutboundStream() {
		return null;
	}
	
	public InboundObjectStream getNewInboundStream() {
		return null;
	}
}
