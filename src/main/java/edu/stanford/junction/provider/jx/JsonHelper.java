package edu.stanford.junction.provider.jx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

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
		Log.d(TAG, "sending msg as " + length);
		Log.d(TAG, "" + message);
		byte[] header = new byte[5];
		header[0] = 'c';
		header[1] = (byte) (length >>> 24);
		header[2] = (byte) ((length << 8) >>> 24);
		header[3] = (byte) ((length << 16) >>> 24);
		header[4] = (byte) ((length << 24) >>> 24);
		
		out.write(header);
		out.write(bytes, 0, bytes.length);		
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

	
    private StringBuilder mStringBuilder = new StringBuilder();
    private int mMessageLength = 0;
    private byte[] leftovers = null;
    private int leftoversOffset = 0;
    private int leftoversCount = 0;
    
    public JSONObject jsonFromStream() throws IOException {
    	String TAG = this.TAG+"-buffer";
    	byte[] buffer = new byte[1024];
    	byte[] inbound;
    	int inboundCount;
    	int inboundOffset = 0;
    	
    	if (leftovers != null) {
    		inbound = leftovers;
    		inboundOffset = leftoversOffset;
    		inboundCount = leftoversCount;
    	} else {
    		inboundCount = in.read(buffer);
    		inboundOffset = 0;
    		inbound = buffer;
    	}
    	
    	do {
    		if (mMessageLength == 0) {
    			if (inboundCount - inboundOffset < 5) { // header length
    				continue;
    			}
    			if (inbound[inboundOffset] != 'c') {
    				String str = new String(inbound, inboundOffset, inboundCount - inboundOffset);
    				throw new IllegalStateException("No length prefix found. Offset: " + inboundOffset + ", count: " + inboundCount +", leftovers: " + (leftovers == inbound));
    			}
    			
    			int size = 0;
        		size |= 0xFF000000 & (inbound[inboundOffset+1] << 24);
        		size |= 0x00FF0000 & (inbound[inboundOffset+2] << 16);
        		size |= 0x0000FF00 & (inbound[inboundOffset+3] << 8);
        		size |= 0x000000FF & inbound[inboundOffset+4];
        		mMessageLength = size;
        		Log.d(TAG, "starting a new json message of length " + size);
        		inboundOffset += 5;
    		}
    		
    		int readLength = Math.min(mMessageLength - mStringBuilder.length(),
    									inboundCount - inboundOffset);
    		
    		Log.d(TAG, "appending bytes: " + new String(inbound, inboundOffset, readLength));
    		mStringBuilder.append(new String(inbound, inboundOffset, readLength));
    		
    		if (mStringBuilder.length() == mMessageLength) {
    			try {
    				String stringRep = mStringBuilder.toString();
    				
    				/* reset state */
    				mStringBuilder.setLength(0);
    				mMessageLength = 0;
    				Log.d(TAG, "leaving offset: " + inboundOffset + ", readLength: " + readLength + ", inboundCount: " + inboundCount);
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
    			inboundOffset = 0;
    		}
    		
    	} while ((inboundCount = in.read(buffer)) > 0);
    	return null;
    }
    
    
    
    
    public JSONObject busted_jsonFromStream() throws IOException {
    	int offset = 0;
    	byte[] buffer = new byte[BUFFER_SIZE];
    	
    	while (true) {
    		int bytes;
    		if (leftovers == null) {
	    		bytes = in.read(buffer);
	    		Log.d(TAG+"-buffer", "fresh read of size " + bytes);
	    		if (bytes < 0) return null;
    		} else {
    			buffer = leftovers;
    			bytes = buffer.length;
    			leftovers = null;
    			Log.d(TAG, "buffer length is....");
    			Log.d(TAG, "........." + buffer.length);
    		}
    		
    		if (mMessageLength == 0) {
    			Log.d(TAG + "-buffer", "from the top!");
    			// read header
    			int size = 0;
        		size |= 0xFF000000 & (buffer[1] << 24);
        		size |= 0x00FF0000 & (buffer[2] << 16);
        		size |= 0x0000FF00 & (buffer[3] << 8);
        		size |= 0x000000FF & buffer[4];
        		mMessageLength = size;
        		Log.d(TAG + "-buffer", "inbound message size " + mMessageLength + ". Read size: " + bytes);
        		
        		if (bytes == 5) {
        			continue;
        		} else {
        			offset = 5;
        		}
    		} else {
    			Log.d(TAG + "-buffer", "from the bottom!!");
    			offset = 0;
    		}
    		
    		int builderLen = mStringBuilder.length();
    		int readTo = Math.min(bytes, mMessageLength - builderLen);
    		Log.d(TAG + "-buffer", "read size is " + readTo);
    		String str = new String(buffer, offset, readTo);
    		
    		mStringBuilder.append(str);
    		Log.d(TAG + "-buffer", "(" + mStringBuilder.length() + ") " + mStringBuilder.toString());
    		if (mMessageLength == mStringBuilder.length()) {
    			try {
    				String stringRep = mStringBuilder.toString();
    				Log.d(TAG, "Returning json " + stringRep);
    				mStringBuilder.setLength(0);
    				mMessageLength = 0;
    				
    				if (bytes != readTo + offset) {
    					Log.d(TAG + "-leftover", "bytes: " + bytes + ", readTo: " + readTo + ", offset:" + offset);
    	    			leftovers = Arrays.copyOfRange(buffer, readTo+1, bytes);
    	    			Log.d(TAG+"-leftover", "still have " + leftovers.length);
    	    		}
    				
    				return new JSONObject(stringRep);
    			} catch (JSONException e) {
    				Log.e(TAG, "Error reading json", e);
    				return null;
    			}
    		}
    	}
    }
}
