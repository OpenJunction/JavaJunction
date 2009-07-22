package edu.stanford.prpl.junction.api.messaging;


import java.util.Map;

import org.json.JSONException;

public class JunctionQuery extends JunctionMessage {

	private String mQuery;
	private String mType;
	private Map<String,Object>mParams = null;
	
	public JunctionQuery(String type, String query) throws JSONException {
		super("{type:\""+type+"\",query:\""+query+"\"}");
		mType=type;
		mQuery=query;
	}
	
	public JunctionQuery(String query) throws JSONException {
		super("{query:\""+query+"\"}");
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