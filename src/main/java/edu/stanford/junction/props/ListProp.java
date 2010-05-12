package edu.stanford.junction.props;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Random;
import java.util.Vector;

public class ListProp extends Prop {

	public ListProp(String propName, String propReplicaName){
		super(propName, new ListState(), propReplicaName);
	}

	public void push(IListItem item){
		addOperation(new PushOp(item));
	}

	public void pop(){
		addOperation(new PopOp());
	}

	public void pushRandom(){
		Vector<String> words = new Vector<String>();
		words.add("dude");
		words.add("apple");
		words.add("hat");
		words.add("cat");
		words.add("barge");
		Random rng = new Random();
		push(new StringItem(words.get(rng.nextInt(words.size()))));
	}

	protected IPropState destringifyState(String s){
		try{	   
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("ListState")){

				JSONArray a = obj.getJSONArray("items");
				Vector<IListItem> items = new Vector<IListItem>();
				for(int i = 0; i < a.length(); i++){
					IListItem item = destringifyItem(a.getString(i));
					items.add(item);
				}
				return new ListState(items);
			}
			else {
				return new ListState();
			}
		}
		catch(JSONException e){
			return new ListState();
		}
	}

	protected IListItem destringifyItem(String s){
		try{
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("StringItem")){
				return new StringItem(obj.optString("text"));
			}
			else{
				return new StringItem("NilItem");
			}
		}
		catch(JSONException e){
			return new StringItem("ErrorItem");
		}
	}

	protected IPropStateOperation destringifyOperation(String s){
		try{
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("pushOp")){
				IListItem item = destringifyItem(obj.getString("item"));
				return new PushOp(item);
			}
			if(type.equals("popOp")){
				return new PopOp();
			}
			else{
				return new NullOp();
			}
		}
		catch(JSONException e){
			return null;
		}
	}

}

class StringItem implements IListItem{

	public String value;

	public StringItem(String value){
		this.value = value;
	}

	public String stringify(){ 
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "StringItem");
			obj.put("text", value);
		}catch(JSONException e){}
		return obj.toString();
	}

	public String toString(){ 
		return "\"" + value + "\"";
	}

	public IListItem copy(){
		return new StringItem(value);
	}

}


class ListState implements IPropState{
	private Vector<IListItem> items;

	public ListState(Vector<IListItem> items){
		this.items = items;
	}

	public ListState(){
		this(new Vector<IListItem>());
	}

	public IPropState applyOperation(IPropStateOperation operation){
		if(operation instanceof PushOp){
			PushOp op = (PushOp)operation;
			return op.applyTo(this);
		}
		else if(operation instanceof PopOp){
			PopOp op = (PopOp)operation;
			return op.applyTo(this);
		}
		else{
			return this;
		}
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
			for(IListItem item : items){
				a.put(item.stringify());
			}
			obj.put("type", "ListState");
			obj.put("items", a);
		}catch(JSONException e){}
		return obj.toString();
	}

	public IPropState copy(){
		Vector<IListItem> newItems = new Vector<IListItem>();
		for(IListItem ea : items){
			newItems.add(ea.copy());
		}
		return new ListState(newItems);
	}

	public void push(IListItem item){
		items.add(item);
	}

	public void insert(IListItem item, int index){
		items.add(index, item);
	}

	public void pop(){
		if(items.size() > 0){
			items.remove(items.size() - 1);
		}
	}

	public String toString(){
		return items.toString();
	}
}

class PushOp implements IPropStateOperation{
	private IListItem item;

	public PushOp(IListItem item){
		this.item = item;
	}

	public ListState applyTo(ListState s){
		ListState newS = (ListState)s.copy();
		newS.push(item);
		return newS;
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "pushOp");
			obj.put("item", item.stringify());
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}


class InsertOp implements IPropStateOperation{

	private int index;
	private IListItem item;

	public InsertOp(IListItem item, int index){
		this.item = item;
		this.index = index;
	}

	public InsertOp(IListItem item){
		this(item, 0);
	}

	public ListState applyTo(ListState s){
		ListState newS = (ListState)s.copy();
		newS.insert(item, index);
		return newS;
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "pushOp");
			obj.put("item", item.stringify());
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}


class PopOp implements IPropStateOperation{

	public ListState applyTo(ListState s){
		ListState newS = (ListState)s.copy();
		newS.pop();
		return newS;
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "popOp");
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}
