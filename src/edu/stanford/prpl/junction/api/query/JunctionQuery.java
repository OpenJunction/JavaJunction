package edu.stanford.prpl.junction.api.query;

import java.util.Map;

public interface JunctionQuery {
	public String getQueryText(); // for evaluator
	public String getTypeID(); // for querier
	public Map<String, Object> getParameterMap();
	
	/*
	 * TODO: I need work!
	 * Things to consider:
	 * * query type
	 * * query ID
	 * * query text
	 * * query parameters
	 * 
	 */
	
	
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

*/