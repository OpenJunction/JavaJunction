package edu.stanford.prpl.junction.api.messaging;

import org.json.JSONException;


public class JunctionEndOfStream extends JunctionMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JunctionEndOfStream() throws JSONException {
		super("{}");
	}
	
	public String getJxMessageType() {
		return "jxEOS";
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