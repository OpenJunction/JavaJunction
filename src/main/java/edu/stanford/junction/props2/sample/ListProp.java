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
import org.json.JSONObject;
import java.util.*;
import edu.stanford.junction.props2.IPropState;
import edu.stanford.junction.props2.IProp;

public class ListProp extends CollectionProp {

	public ListProp(String propName, String propReplicaName, IPropState s, long seqNum){
		super(propName, propReplicaName, s, seqNum);
	}

	public ListProp(String propName, String propReplicaName, IPropState s){
		this(propName, propReplicaName, s, 0);
	}

	public ListProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new ListState());
	}

	public IProp newFresh(){
		return new ListProp(getPropName(), getPropReplicaName(), newState());
	}

}


