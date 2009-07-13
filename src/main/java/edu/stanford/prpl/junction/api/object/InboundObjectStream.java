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
	 * This function reads an object from the stream.
	 * The method blocks while waiting for objects.
	 * 
	 * @return the object received. 
	 *  
	 * @throws IOException on serialization or reception error
	 */
	public Object receive() throws IOException;
	

	
	/**
	 * Waits for an object to become available,
	 * blocking the thread from executing.
	 * 
	 * @return true if an object is available and
	 * false otherwise (if the stream has been closed
	 * by the sender)
	 */
	public boolean waitForObject();
	
	/**
	 * Returns true if there is an object ready to be read,
	 * in which case a call to receive() will return immediately.
	 * 
	 * @return true if an object can be immediately read.
	 */
	public boolean hasObject();
	
	/**
	 * This function closes the PrPl object sender
	 */
	public void close();
}
