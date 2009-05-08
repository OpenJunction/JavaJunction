package edu.stanford.prpl.junction.api.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


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
