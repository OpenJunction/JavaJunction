package edu.stanford.junction.props.sample;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Collections;
import java.util.Iterator;
import edu.stanford.junction.props.Prop;
import edu.stanford.junction.props.IPropState;
import edu.stanford.junction.props.IPropStateOperation;
import edu.stanford.junction.props.NullOp;
import edu.stanford.junction.props.IStringifiable;

public class SetProp extends Prop {

	ISetItemBuilder builder;

	public SetProp(String propName, String propReplicaName, ISetItemBuilder builder, Set<ISetItem> items){
		super(propName, new SetState(items), propReplicaName);
		this.builder = builder;
	}

	public SetProp(String propName, String propReplicaName, ISetItemBuilder builder){
		this(propName, propReplicaName, builder, new HashSet<ISetItem>());
	}

	public SetProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new StringSetItemBuilder());
	}

	public void add(ISetItem item){
		addOperation(new AddOp(item));
	}

	public void delete(ISetItem item){
		addOperation(new DeleteOp(item));
	}

	public Set items(){
		SetState s = (SetState)getState();
		return s.unmodifiableSet();
	}

	// Debug
	public void doRandom(){
		Vector<String> words = new Vector<String>();
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
			add(new StringSetItem(words.get(rng.nextInt(words.size()))));
		}
		else{
			Iterator it = items().iterator();
			if(it.hasNext()){
				delete((ISetItem)it.next());
			}
		}
	}

	protected IPropState destringifyState(String s){
		try{	   
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("SetState")){

				JSONArray a = obj.getJSONArray("items");
				Set<ISetItem> items = new HashSet<ISetItem>();
				for(int i = 0; i < a.length(); i++){
					ISetItem item = builder.destringify(a.getString(i));
					items.add(item);
				}
				return new SetState(items);
			}
			else {
				return new SetState();
			}
		}
		catch(JSONException e){
			return new SetState();
		}
	}

	protected IPropStateOperation destringifyOperation(String s){
		try{
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("addOp")){
				ISetItem item = builder.destringify(obj.getString("item"));
				return new AddOp(item);
			}
			else if(type.equals("deleteOp")){
				ISetItem item = builder.destringify(obj.getString("item"));
				return new DeleteOp(item);
			}
			else{
				return new NullOp();
			}
		}
		catch(JSONException e){
			return null;
		}
	}

	public static interface ISetItemBuilder{
		ISetItem destringify(String s);
	}

	public interface ISetItem extends IStringifiable{
		ISetItem copy();
	}

}


class StringSetItemBuilder implements SetProp.ISetItemBuilder{
	public SetProp.ISetItem destringify(String s){
		try{
			JSONObject obj = new JSONObject(s);
			return new StringSetItem(obj.optString("text"));
		}
		catch(JSONException e){
			return new StringSetItem("");
		}		
	}
}

class StringSetItem implements SetProp.ISetItem{

	public String value;

	public StringSetItem(String value){
		this.value = value;
	}

	public String stringify(){ 
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "StringSetItem");
			obj.put("text", value);
		}catch(JSONException e){}
		return obj.toString();
	}

	public int hashCode(){
		return value.hashCode();
	}

	public boolean equals(Object obj) {
		if(obj instanceof SetProp.ISetItem){
			return ((StringSetItem)obj).value.equals(value);
		}
		return false;
	}

	public String toString(){ 
		return "\"" + value + "\"";
	}

	public SetProp.ISetItem copy(){
		return new StringSetItem(value);
	}

}


class SetState implements IPropState{
	private Set<SetProp.ISetItem> items;

	public SetState(Set<SetProp.ISetItem> items){
		this.items = new HashSet(items);
	}

	public SetState(){
		this(new HashSet<SetProp.ISetItem>());
	}

	public IPropState applyOperation(IPropStateOperation operation){
		if(operation instanceof AddOp){
			AddOp op = (AddOp)operation;
			return op.applyTo(this);
		}
		else if(operation instanceof DeleteOp){
			DeleteOp op = (DeleteOp)operation;
			return op.applyTo(this);
		}
		else{
			return this;
		}
	}

	public Set unmodifiableSet(){
		return Collections.unmodifiableSet(items);
	}

	public IPropStateOperation nullOperation(){
		return new NullOp();
	}

	public String hash(){
		return items.toString();
	}

	public String stringify(){
		JSONObject obj = new JSONObject();
		try{
			JSONArray a = new JSONArray();
			for(SetProp.ISetItem item : items){
				a.put(item.stringify());
			}
			obj.put("type", "SetState");
			obj.put("items", a);
		}catch(JSONException e){}
		return obj.toString();
	}

	public IPropState copy(){
		SetState s = new SetState();
		for(SetProp.ISetItem ea : items){
			s.add(ea.copy());
		}
		return s;
	}

	public void add(SetProp.ISetItem item){
		items.add(item);
	}

	public void delete(SetProp.ISetItem item){
		items.remove(item);
	}

	public String toString(){
		return items.toString();
	}
}

class AddOp implements IPropStateOperation{
	private SetProp.ISetItem item;

	public AddOp(SetProp.ISetItem item){
		this.item = item;
	}

	public SetState applyTo(SetState s){
		SetState newS = (SetState)s.copy();
		newS.add(item);
		return newS;
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "addOp");
			obj.put("item", item.stringify());
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}

class DeleteOp implements IPropStateOperation{
	private SetProp.ISetItem item;

	public DeleteOp(SetProp.ISetItem item){
		this.item = item;
	}

	public SetState applyTo(SetState s){
		SetState newS = (SetState)s.copy();
		newS.delete(item);
		return newS;
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "deleteOp");
			obj.put("item", item.stringify());
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}

