package edu.stanford.prpl.junction.api.messaging;

import org.json.JSONException;
import org.json.JSONObject;


public abstract class JunctionMessage extends JSONObject {
	public static final String JX_MESSAGE_TYPE = "jxMessageType";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public JunctionMessage(String json) throws JSONException {
		super(json);
		put("jxMessageType",getJxMessageType());
	}
	
	// Required for deserialization
	public abstract String getJxMessageType();
	
	/*
	public static boolean isJunctionMessage(Message message) {

		if (message.getData() == null) {
			return false;
		}
		
		return ((HashMap<String,Object>)(message.getData())).containsKey("jxMessageType");
	}*/
	/*
	public static JunctionMessage load(Message message) {
		try {

			HashMap<String,Object>data = (HashMap<String,Object>)message.getData();
			JunctionMessage jMsg = null;

			if ("jxeos".equalsIgnoreCase((String)data.get("jxMessageType"))) {
				// end of stream
				jMsg = new JunctionEndOfStream();
				jMsg.putAll(message);
			}
			
			if ("jxquery".equalsIgnoreCase((String)data.get("jxMessageType"))) {
				jMsg = new JunctionQuery();
				jMsg.putAll(message);
				jMsg.loadMap(data);
			}

			return jMsg;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	*/
	
}
