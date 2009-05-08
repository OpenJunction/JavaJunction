package edu.stanford.prpl.junction.api.object;

import java.io.IOException;


/**
 * This class represents objects that may or may not be serialized.
 * If serialization is required, the JSON format is used.
 * 
 * The stream may be usable with optimized serialization, 
 * if the receiver is a JAVA program, for example.
 * 
 * @author Matthew Nasielski
 * @author Ben Dodson
 *
 */

public interface OutboundObjectStream {        
	/**
	 * This function sends a PrPl object to the stream
	 * @return the object to be sent
	 * @throws IOException on serialization or reception error
	 */
	public void send(Object outbound) throws IOException;
	
	/**
	 * This function closes the PrPl object sender
	 */
	public void close();
	
	/**
	 * Indicates that this is a good time to flush the stream
	 */
	public void flush();
}
