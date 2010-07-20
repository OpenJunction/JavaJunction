package edu.stanford.junction.props2.sample;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.*;
import edu.stanford.junction.props2.Prop;
import edu.stanford.junction.props2.IPropState;

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
				a.put(item.getRaw());
			}
			obj.put("items", a);
			System.out.println("ARRAY: " + a.toString());
		}
		catch(JSONException e){}
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



