package edu.stanford.junction.provider.jx.json;

import java.io.IOException;
import org.json.JSONObject;


/**
 * Helps read and write json messages over a socket
 * by handling chunking for both reads and writes.
 *
 */
public abstract class JsonHandler {
	protected final String TAG;
	static int count = 0;
	
	public JsonHandler() {
		TAG = "json-"+(count++);
	}
	
	public abstract void sendJson(JSONObject message) throws IOException;
    public abstract JSONObject jsonFromStream() throws IOException;
}
