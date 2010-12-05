package edu.stanford.junction.provider.jx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import javax.naming.OperationNotSupportedException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.provider.jx.JXServer.Log;

/**
 * Helps read and write json messages over a socket
 * by handling chunking for both reads and writes.
 *
 */
public class JsonHelper {
	final String TAG;
	private static final int BUFFER_SIZE = 1024;
	static int count = 0;
	
	private final OutputStream out;
	private final InputStream in;
	
	public JsonHelper(InputStream in, OutputStream out) {
		TAG = "json-"+(count++);
		this.in = in;
		this.out = out;
	}
	
	/*
	public void sendJson(JSONObject message) throws IOException {
		byte[] bytes = message.toString().getBytes();
    	int length = bytes.length;
		out.write(bytes, 0, length);
		out.flush();
		
	}*/
	
	// not working
	public void sendJson(JSONObject message) throws IOException {
    	byte[] bytes = message.toString().getBytes();
    	int length = bytes.length;
    	
		byte[] header = new byte[5];
		header[0] = 'c';
		header[1] = (byte) (length >>> 24);
		header[2] = (byte) ((length << 8) >>> 24);
		header[3] = (byte) ((length << 16) >>> 24);
		header[4] = (byte) ((length << 24) >>> 24);
		
		out.write(header);
		out.write(bytes, 0, bytes.length);
		out.flush();
    }
	
	/*
	public JSONObject jsonFromStream() throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytes = in.read(buffer);
		if (bytes > 0) {
			try {
				return new JSONObject(new String(buffer, 0, bytes));
			} catch (JSONException e) {
				Log.e(TAG, "error parsing json", e);
			}
		}
		return null;
	}*/

	private byte[] byteBuffer = new byte[BUFFER_SIZE];
	private int byteCount;
    private int mMessageLength = 0;
    private byte[] leftovers = null;
    private int leftoversOffset = 0;
    private int leftoversCount = 0;
    
    public JSONObject jsonFromStream() throws IOException {
    	String TAG = this.TAG+"-buffer";
    	byte[] buffer = new byte[BUFFER_SIZE];
    	byte[] inbound;
    	int inboundCount;
    	int inboundOffset = 0;
    	
    	try {
	    	if (leftovers != null) {
	    		inbound = leftovers;
	    		inboundOffset = leftoversOffset;
	    		inboundCount = leftoversCount;
	    	} else {
	    		int available = Math.min(buffer.length, in.available());
	    		if (available > 0) {
	    			inboundCount = in.read(buffer, 0, available);
	    		} else {
	    			inboundCount = in.read(buffer);
	    		}
	    		inboundOffset = 0;
	    		inbound = buffer;
	    	}
    	
	    	do {
	    		if (mMessageLength == 0) {
	    			if (inboundCount - inboundOffset < 5) { // header length
	    				continue;
	    			}
	    			
	    			if (inbound[inboundOffset] != 'c') {
	    				throw new IllegalStateException("No length prefix found. Offset: " + inboundOffset + ", count: " + inboundCount +", leftovers: " + (leftovers == inbound));
	    			}
	    			
	    			int size = 0;
	        		size |= 0xFF000000 & (inbound[inboundOffset+1] << 24);
	        		size |= 0x00FF0000 & (inbound[inboundOffset+2] << 16);
	        		size |= 0x0000FF00 & (inbound[inboundOffset+3] << 8);
	        		size |= 0x000000FF & inbound[inboundOffset+4];
	        		mMessageLength = size;
	
	        		inboundOffset += 5;
	        		byteCount = 0;
	        		if (mMessageLength > byteBuffer.length) {
	        			byteBuffer = new byte[mMessageLength];
	        		}
	    		}
	    		
	    		int readLength = Math.min(mMessageLength - byteCount,
	    									inboundCount - inboundOffset);
	
	    		System.arraycopy(inbound, inboundOffset, byteBuffer, byteCount, readLength);
	    		byteCount += readLength;
	    		if (byteCount == mMessageLength) {
	    			try {
	    				String stringRep = new String(byteBuffer, 0, byteCount);
	    				
	    				/* reset state */
	    				mMessageLength = 0;
	    				if (inboundOffset + readLength != inboundCount) {
		    				leftovers = inbound;
		    				leftoversOffset = inboundOffset +  readLength;
		    				leftoversCount = inboundCount;
	    				} else {
	    					leftovers = null;
	    				}
		    			
	    				return new JSONObject(stringRep);
	    			} catch (JSONException e) {
	    				Log.e(TAG, "Error reading json", e);
	    				return null;
	    			}
	    		} else {
	    			leftovers = null;
	    			inbound = buffer;
	    			inboundOffset = 0;
	    		}
	    		
	    		int available = Math.min(buffer.length, in.available());
	    		if (available > 0) {
	    			inboundCount = in.read(buffer, 0, available);
	    		} else {
	    			inboundCount = in.read(buffer);
	    		}
	    	} while (inboundCount > 0);
    	} catch (SocketException e) {
 			Log.d(TAG, "Socket closed.");
    	}
    	return null;
    }
}
