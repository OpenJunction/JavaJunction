package edu.stanford.junction.provider.jx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
	
	public void sendJson(JSONObject message) throws IOException {
		byte[] bytes = message.toString().getBytes();
    	int length = bytes.length;
		out.write(bytes, 0, length);
		out.flush();
		
	}
	
	// not working
	public void sendJsonBuffered(JSONObject message) {
    	byte[] bytes = message.toString().getBytes();
    	int length = bytes.length;
    	try {
    		Log.d(TAG, "sending msg as " + length);
    		byte[] header = new byte[5];
    		header[0] = 'c';
    		header[1] = (byte) (length >>> 24);
    		header[2] = (byte) ((length << 8) >>> 24);
    		header[3] = (byte) ((length << 16) >>> 24);
    		header[4] = (byte) ((length << 24) >>> 24);
    		
    		out.write(header);
    		out.write(bytes, 0, bytes.length);
    		
    		/*
        	int chunks = (bytes.length / BUFFER_LENGTH);
        	if (length % BUFFER_LENGTH != 0) chunks++;
        	
        	if (chunks > 1) {
        		Log.d(TAG, "sending msg as " + length);
        		byte[] header = new byte[5];
        		header[0] = 'c';
        		header[1] = (byte) (length >>> 24);
        		header[2] = (byte) ((length << 8) >>> 24);
        		header[3] = (byte) ((length << 16) >>> 24);
        		header[4] = (byte) ((length << 24) >>> 24);
        		
        		out.write(header);
        	}
        	
        	int idx;
        	for (idx = 0; idx < (chunks - 1); idx++) {
        		out.write(bytes, idx*BUFFER_LENGTH, BUFFER_LENGTH);
        	}
        	int len = bytes.length - idx*BUFFER_LENGTH;
        	out.write(bytes, idx*BUFFER_LENGTH, len);
        	*/
    		
    	} catch (IOException e) {
    		Log.e(TAG, "Exception during write", e);
    	}
    }
	
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
	}

	/*
    private StringBuilder mStringBuilder = new StringBuilder();
    private int mMessageLength;

    public JSONObject jsonFromStream() {
    	int offset = 0;
    	if (mMessageLength == 0) {
    		/*
    		if (bytes[0] != 'c') {
    			try {
    				return new JSONObject(new String(bytes, 0, len));
    			} catch (JSONException e) {
    				Log.e(TAG, "JSON did not parse.", e);
    				return null;
    			}
    		}
    		*//*
    		
    		int size = 0;
    		size |= 0xFF000000 & (bytes[1] << 24);
    		size |= 0x00FF0000 & (bytes[2] << 16);
    		size |= 0x0000FF00 & (bytes[3] << 8);
    		size |= 0x000000FF & bytes[4];
    		
    		Log.d(TAG, "Reading string of length " + size + ". This read() has " + len + " bytes");
    		// Start a new chunked message
    		mMessageLength = size;
    		offset = 5;
    		len -= offset;
    		if (len == 0) return null;
    	}
    	
		mStringBuilder.append(new String(bytes, offset, len));
		Log.d(TAG, "string of length " + mStringBuilder.length() + " is " + mStringBuilder);
		if (mStringBuilder.length() == mMessageLength) {
			try {
				String stringRep = mStringBuilder.toString();
				Log.d(TAG, "Returning json " + stringRep);
				mStringBuilder.setLength(0);
				mMessageLength = 0;
				return new JSONObject(stringRep);
			} catch (JSONException e) {
				Log.e(TAG, "Error reading json", e);
				return null;
			}
		} else {
			return null;
		}
    	
    }*/

}
