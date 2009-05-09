//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package edu.stanford.prpl.junction.api;

import java.net.URL;

import org.json.JSONObject;

import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.api.query.JunctionQuery;
import edu.stanford.prpl.junction.api.query.JunctionQueryHandler;


/**
 * The API for the JAVA-Junction library.
 * Features include:
 * 	Standard Bayeux pub/sub
 * 	Support for queries and named resources
 * 
 * 
 */

public interface JunctionAPI
{
	
	// query
		// stored query
		// direct query
		// query for resource(?)
		// 
		// queryBuilder
		// generic query abstraction
	
	// todo: need reference to query target in query methods
	// need reference to query event in handler
	
	
	

	
	/**************************************8
	 * 
	 * 


question: how to specify query target? A few options:

1.
jm.query(query, target, callback);

2.
query.setTarget(...);

3.
JunctionActor actor = jm.getActor("DatalogDB");
actor.query(query, callback);

could do multiple targets here, but for now that's best handled 
by letting the underlying DB engine handle it (EG distributed datalog)


	 * 
	 * 
	 * 
	 */
	
	
	
	
	
	
	
	
	public void query(JunctionQuery query, JunctionCallback callback);
	public void query(JunctionQuery query, String channelName);
	public InboundObjectStream query(JunctionQuery query);
	
		// supports persistent and one-off ?????
	// public void query(JunctionStoredQuery query, JunctionCallback callback);
	
	// have another yeild-like method where you can wait for a set of
	// resources to finish loading, and then proceed.
	// eg, get genres, get random albums, then build GUI once they're all ready
	// or once certain pieces are ready
	public void registerQueryHandler(JunctionQueryHandler handler);
	
	
	// JunctionQuery
	
	/**
	 * 
	 * jm.query("SELECT name FROM jz_nodes WHERE ptype='artist'", 
	 * 		new JunctionCallback(){
	 * 			public void onMessageReceived(obj) { // not string
	 * 				
	 * 			}
	 * 		}
	 * 
	 * jm.onReady([res1, res2 ,res3], function(res(?)) {} );
	 * 
	 */
	
	// respond
	// public JunctionResponder routeQuery(String query); 
			// ???????? JR == enum, routing to one of :: 
	// public Something queryResponse(String query); // one-off
	// public void queryResponse(String query, (JSON?)OutputStream response) // persistent
	
	// subscribe
		// message
		// stream
	
	// publish
		// content:
			// message
			// stream
	
		// channel:
			// actor
			// client
			// session
			// channel
			
	// numClients
		// total
		// of type X
	
	// getClientID
	
	// getSessionID
	
	// getActors
}
