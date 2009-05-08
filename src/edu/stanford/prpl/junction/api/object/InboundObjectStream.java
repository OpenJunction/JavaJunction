package edu.stanford.prpl.junction.api.object;

import java.io.IOException;


/**
 * This class is used to receive objects sent
 * via an OutboundObjectStream.
 * 
 * @author Matthew Nasielski
 * @author Ben Dodson
 */

public interface InboundObjectStream {        
	/**
	 * This function sends a PrPl object to the stream
	 * @return the object to be sent
	 * @throws IOException on serialization or reception error
	 */
	public Object receive() throws IOException;
	
	/**
	 * This function closes the PrPl object sender
	 */
	public void close();
}
