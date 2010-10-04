/*
 * Copyright (C) 2010 Stanford University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package edu.stanford.junction.props2.sample;
import edu.stanford.junction.props2.IPropState;
import edu.stanford.junction.props2.IProp;
import org.json.JSONObject;
import java.util.*;

public class SetProp extends CollectionProp {

	public SetProp(String propName, String propReplicaName, Collection<JSONObject> items){
		super(propName, propReplicaName, new SetState(items));
	}

	public SetProp(String propName, String propReplicaName, IPropState s){
		super(propName, propReplicaName, s);
	}

	public SetProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new ArrayList<JSONObject>());
	}

	public IProp newFresh(){
		return new SetProp(getPropName(), getPropReplicaName(), newState());
	}
}


class SetState extends CollectionState{

	public SetState(Collection<JSONObject> inItems){
		super(inItems);
	}

	public SetState(){
		this(new ArrayList<JSONObject>());
	}

	public Collection<JSONObject> items(){
		return Collections.unmodifiableSet((Set)items);
	}

	protected CollectionState newWith(Collection<JSONObject> items){
		return new SetState(items);
	}

	protected Collection<JSONObject> newImplCollection(){
		return new HashSet<JSONObject>();
	}

}



