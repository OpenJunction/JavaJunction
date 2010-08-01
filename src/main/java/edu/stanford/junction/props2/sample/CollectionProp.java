package edu.stanford.junction.props2.sample;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.*;
import edu.stanford.junction.props2.Prop;
import edu.stanford.junction.props2.IPropState;


abstract public class CollectionProp extends Prop {

	public CollectionProp(String propName, String propReplicaName, IPropState state){
		super(propName, state, propReplicaName);
	}

	protected IPropState newStateWith(Collection<JSONObject> items){
		return ((CollectionState)getState()).newWith(items);
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
		CollectionState s = (CollectionState)getState();
		return s.items();
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


