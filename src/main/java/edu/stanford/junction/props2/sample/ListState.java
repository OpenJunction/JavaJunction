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


public class ListState extends CollectionState{

	public ListState(Collection<JSONObject> inItems){
		super(inItems);
	}

	public ListState(){
		this(new ArrayList<JSONObject>());
	}

	public Collection<JSONObject> items(){
		return Collections.unmodifiableList((List)items);
	}

	protected CollectionState newWith(Collection<JSONObject> items){
		return new ListState(items);
	}

	protected Collection<JSONObject> newImplCollection(){
		return new ArrayList<JSONObject>();
	}

}



