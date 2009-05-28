package edu.stanford.prpl.junction.api.messaging;

import java.util.HashMap;
import java.util.Map;

public class JunctionQuery extends JunctionMessage {

	private String mQuery;
	private String mType;
	private Map<String,Object>mParams = null;
	
	public JunctionQuery() {
		
	}
	
	public JunctionQuery(String type, String query) {
		mType=type;
		mQuery=query;
	}
	
	public JunctionQuery(String query) {
		mQuery=query;
	}
	
	public String getQueryText() {
		return mQuery;
	}

	public String getQueryType() {
		return mType;
	}

	public void setQueryType(String type) {
		mType=type;
	}

	public Map<String, Object> getParameterMap() {
		return mParams;
	}
	
	public void setParameterMap(Map<String,Object>map) {
		mParams=map;
	}

	public String getJxMessageType() {
		return "jxquery";
	}
	
	@Override
	public void loadMap(Map<String,Object> data) {
		mQuery = (String)data.get("queryText");
		mType = (String)data.get("queryType");
		if (data.containsKey("queryParams")) {
			mParams = (HashMap<String,Object>)data.get("queryParams");
		}
	}
	
	public Map<String,Object> getMap() {
		Map<String,Object>map = super.getMap();
		map.put("queryText", mQuery);
		map.put("queryType",mType);
		if (null != mParams) {
			map.put("queryParams",mParams);
		}
		
		return map;
	}
}

/*


---------------
-- IN CLIENT --
---------------

mJuntionManager = new JunctionManager(...);
// JunctionQuery query = new PlaintextQuery("SELECT * FROM photos");
//JunctionQuery query = new StoredQuery("GET_PHOTOS");

// we can auto-generate a source file for JxActorDatalogServer given
// a JSON activity (or actor) description
JunctionQuery query = JxActor_DatalogServer.queries.GET_PHOTOS;
mJunctionManager.query(query, mJunctionManager.getClientChannel());


----------------
-- JAVASCRIPT --
----------------

jm.query("SELECT * FROM photos", "/my/channel");
results = jm.query("..."); // differentiate by arg count

*/