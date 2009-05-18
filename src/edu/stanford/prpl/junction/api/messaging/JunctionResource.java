package edu.stanford.prpl.junction.api.messaging;

import java.util.Map;



public class JunctionResource extends JunctionMessage {

	@Override
	public String getJxMessageType() {
		return "jxresource";
	}

	@Override
	public void loadMap(Map<String,Object>data) {
	
	}

	@Override
	public Map<String, Object> getMap() {
		// TODO Auto-generated method stub
		return null;
	}
}
