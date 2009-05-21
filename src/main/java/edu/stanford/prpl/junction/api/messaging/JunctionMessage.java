package edu.stanford.prpl.junction.api.messaging;

import java.util.HashMap;
import java.util.Map;

public abstract class JunctionMessage {
	public static final String JX_MESSAGE_TYPE = "jxMessageType";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	// Required for deserialization
	public abstract String getJxMessageType();	
	
	public Map<String,Object> getMap() {
		Map<String,Object>map = new HashMap<String,Object>();
		map.put("jxMessageType",getJxMessageType());
		return map;
	}
	
	public abstract void loadMap(Map<String,Object>map);
	
	public static JunctionMessage load(Map<String, Object> data) {
		try {
			JunctionMessage message = null;

			if ("jxquery".equals(data.get("jxMessageType"))) {
				message = new JunctionQuery();
				message.loadMap(data);
			}
			
			return message;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
