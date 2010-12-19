package edu.stanford.junction.provider.jx.json;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.provider.jx.JXServer.Log;

/**
 * Helps read and write json messages over a WebSocket.
 *
 */
public class JsonWebSocketHandler extends JsonHandler {
	private static final int BUFFER_SIZE = 1024;
	
	private final OutputStream out;
	private final InputStream in;
	
	public JsonWebSocketHandler(InputStream in, OutputStream out) {
		this.in = in;
		this.out = new BufferedOutputStream(out);
	}

	public void sendJson(JSONObject message) throws IOException {
    	byte[] bytes = message.toString().getBytes();
    	out.write(0x00);
		out.write(bytes, 0, bytes.length);
		out.write(0x000000FF);
		out.flush();
    }

	private byte[] buffer = new byte[BUFFER_SIZE];
	private int byteCount;
	
	/**
	 * Reads a JSON object from the handler's  inputStream.
	 * This method is not thread safe.
	 */
    public JSONObject jsonFromStream() throws IOException {
		while (true) {
            try {
            	byteCount = in.read(buffer);
            	if (byteCount == -1) {
            		break;
            	}
            	
            	if (buffer[0] != 0x00) {
            		Log.e(TAG, "Bad frame header found in WebSocket connection");
            		continue;
            	}
            	
            	if (buffer[byteCount-1] != (byte)0x000000FF) {
            		Log.e(TAG, "Bad frame footer found in WebSocket connection");
            		continue;
            	}
            	
            	String str = new String(buffer, 1, byteCount - 2);
            	Log.d(TAG, "read " + str);
                JSONObject json = new JSONObject(str);
                
                if (json == null) {
                	continue; // break?
                }
                
                return json;
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                //connectionLost();
                break;
            } catch (JSONException e) {
				Log.e(TAG, "Not a JSON message." + e);
			}
        }
		return null;
    }
}
