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
import org.json.JSONArray;
import org.json.JSONException;
import java.util.*;
import edu.stanford.junction.props2.Prop;
import edu.stanford.junction.props2.IWithStateAction;
import edu.stanford.junction.props2.IPropState;


abstract public class CollectionProp extends Prop {

	public CollectionProp(String propName, String propReplicaName, IPropState state, long seqNum){
		super(propName, propReplicaName, state, seqNum);
	}

	public CollectionProp(String propName, String propReplicaName, IPropState state){
		super(propName, propReplicaName, state, 0);
	}

	protected IPropState newStateWith(final Collection<JSONObject> items){
		return withState(new IWithStateAction<IPropState>(){
				public IPropState run(IPropState state){
					return ((CollectionState)state).newWith(items);
				}
			});
	}

	protected IPropState newState(){
		return newStateWith(new ArrayList<JSONObject>());
	}

	public void add(JSONObject item){
		addOperation(newAddOp(item));
	}

	public void replace(JSONObject item1, JSONObject item2){
		addOperation(newReplaceOp(item1, item2));
	}

	public void delete(JSONObject item){
		addOperation(newDeleteOp(item));
	}

	public void clear(){
		addOperation(newClearOp());
	}

	public Collection<JSONObject> items(){
		return withState(new IWithStateAction<Collection<JSONObject>>(){
				public Collection<JSONObject> run(IPropState state){
					return ((CollectionState)state).items();
				}
			});
	}

	// Debug
	public void doRandom(){
		ArrayList<String> words = new ArrayList<String>();
		words.add("dude");
		words.add("apple");
		words.add("hat");
		words.add("cat");
		words.add("barge");
		words.add("horse");
		words.add("mango");
		words.add("code");
		Random rng = new Random();
		if(rng.nextInt(2) == 0){
			JSONObject item = new JSONObject();
			String word = words.get(rng.nextInt(words.size()));
			try{
				item.put("id", word.hashCode());
				item.put("str", word);
				add(item);
			}
			catch(JSONException e){}
		}
		else{
			Iterator<JSONObject> it = items().iterator();
			if(it.hasNext()){
				delete(it.next());
			}
		}
	}

	protected IPropState reifyState(JSONObject obj){
		JSONArray a = obj.optJSONArray("items");
		ArrayList<JSONObject> items = new ArrayList<JSONObject>();
		for(int i = 0; i < a.length(); i++){
			items.add(a.optJSONObject(i));
		}
		return newStateWith(items);
	}

	protected JSONObject newAddOp(JSONObject item){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "addOp");
			obj.put("item", item);
		}catch(JSONException e){}
		return obj;
	}

	protected JSONObject newDeleteOp(JSONObject item){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "deleteOp");
			obj.put("item", item);
		}catch(JSONException e){}
		return obj;
	}

	protected JSONObject newReplaceOp(JSONObject item1, JSONObject item2){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "replaceOp");
			obj.put("item1", item1);
			obj.put("item2", item2);
		}catch(JSONException e){}
		return obj;
	}

	protected JSONObject newClearOp(){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "clearOp");
		}catch(JSONException e){}
		return obj;
	}

}


