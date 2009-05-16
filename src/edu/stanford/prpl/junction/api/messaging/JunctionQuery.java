package edu.stanford.prpl.junction.api.messaging;

import java.util.Map;

public abstract class JunctionQuery extends JunctionMessage {
	public abstract String getQueryText();
	public abstract String getQueryType();
	public abstract Map<String, Object> getParameterMap();
	public abstract boolean isPersistent();
	public abstract void persist(boolean shouldI);
	
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