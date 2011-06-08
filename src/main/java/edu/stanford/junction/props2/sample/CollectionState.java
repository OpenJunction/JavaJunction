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
import edu.stanford.junction.props2.IPropState;
import edu.stanford.junction.addon.JSONObjWrapper;

abstract class CollectionState implements IPropState{

	protected Collection<JSONObject> items;
	protected int hashcode = 0;

	public CollectionState(Collection<JSONObject> inItems){
		this.items = newImplCollection();
		for(JSONObject ea : inItems){
			add(new JSONObjWrapper(ea));
		}
		updateHashCode();
	}

	abstract protected CollectionState newWith(Collection<JSONObject> items);
	abstract protected Collection<JSONObject> newImplCollection();


	public void add(JSONObject obj){
		this.items.add(obj);
	}

	public void delete(JSONObject obj){
		this.items.remove(obj);
	}

	public void replace(JSONObject obj1, JSONObject obj2){
		this.items.remove(obj1);
		this.items.add(obj2);
	}

	public void clear(){
		this.items.clear();
	}

	public IPropState applyOperation(JSONObject obj){
		String type = obj.optString("type");
		if(type.equals("addOp")){
			add(new JSONObjWrapper(obj.optJSONObject("item")));
		}
		else if(type.equals("deleteOp")){
			delete(new JSONObjWrapper(obj.optJSONObject("item")));
		}
		else if(type.equals("replaceOp")){
			replace(new JSONObjWrapper(obj.optJSONObject("item1")),
					new JSONObjWrapper(obj.optJSONObject("item2")));
		}
		else if(type.equals("clearOp")){
			clear();
		}
		else{
			System.err.println("Unrecognized operation: " + type);
		}

		updateHashCode();
		return this;
	}

	protected void updateHashCode(){
		this.hashcode = 1;
		for(JSONObject ea : this.items){
			this.hashcode ^= ea.hashCode();
		}
	}

	public int hashCode(){
		return this.hashcode;
	}

	abstract public Collection<JSONObject> items();

	public JSONObject toJSON(){
		JSONObject obj = new JSONObject();
		try{
			JSONArray a = new JSONArray();
			for(JSONObject o : this.items){
				JSONObjWrapper item = (JSONObjWrapper)o;
				JSONObject raw = item.getRaw();
				// TODO:  Horribly inefficient, but fixes a bug associated with stored state.
				a.put(new JSONObject(raw.toString()));
			}
			obj.put("items", a);
		}
		catch(JSONException e){
		    e.printStackTrace();
		}
		return obj;
	}

	public IPropState copy(){
		ArrayList<JSONObject> all = new ArrayList<JSONObject>();
		for(JSONObject ea : this.items){
			all.add((JSONObject)((JSONObjWrapper)ea).clone());
		}
		return newWith(all);
	}

	public String toString(){
		return this.items.toString();
	}
}



