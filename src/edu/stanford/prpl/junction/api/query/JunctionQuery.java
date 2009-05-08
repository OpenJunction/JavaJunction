package edu.stanford.prpl.junction.api.query;

public interface JunctionQuery {
	public String getQueryText(); // for evaluator
	public String getTypeID(); // for querier
	
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
-- IN DATALOG --
----------------

// actor type ~ datalogServer
JunctionManager mJunctionManager = new JunctionManager(...);
Resource handlerID = 
  mJunctionManager.registerQueryHandler(
  	
  	new JunctionQueryHandler() {

  
  public boolean supportsQuery(JunctionQuery query) {
  	return DatalogStoredQuery.supports(query);
  }
  
  public void handleQuery(JunctionQuery query, JSONOutputStream results) {
	// query text:
	if (!(query instanceof StoredQuery)) {
		results.close(); // todo: set error message
		return;
	}
	String queryText = query.getQueryText();
	
	// get results
	// when result comes in:
	results.write(jsonObject.getBytes());
	
	// no more results
	results.close();
}

-- OR: --

class DatalogActor extends Junction.Actor {
	public List<JunctionQueryHandler> getQueryHandlers() { ...
}

JunctionManager.registerActor(DatalogActor.getInstance());


abstract class Junction.Actor {
	public JSONObject getDescriptor();
	// queries: use QueryHandler.supportID() to populate


	public Junction.Actor getInstance();
}


*/