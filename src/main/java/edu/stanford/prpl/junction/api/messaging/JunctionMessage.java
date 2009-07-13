package edu.stanford.prpl.junction.api.messaging;

import java.util.HashMap;
import java.util.Map;

import org.cometd.Message;
import org.cometd.server.MessageImpl;

public abstract class JunctionMessage extends MessageImpl {
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
	
	public static boolean isJunctionMessage(Message message) {
		// hack for aggressive pooling
        ((MessageImpl)message).incRef();
		if (message.getData() == null) {
			return false;
		}
		
        ((MessageImpl)message).incRef();

		return ((HashMap<String,Object>)(message.getData())).containsKey("jxMessageType");
	}
	
	public static JunctionMessage load(Message message) {
		try {
			// hack for aggressive pooling
	        ((MessageImpl)message).incRef();
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
	
}
