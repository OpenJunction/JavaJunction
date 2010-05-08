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

	public void push(IStringifiable item){
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
				Vector<IStringifiable> items = new Vector<IStringifiable>();
				for(int i = 0; i < a.length(); i++){
					IStringifiable item = destringifyItem(a.getString(i));
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

	protected IStringifiable destringifyItem(String s){
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
			long nonce = obj.optLong("nonce");
			String type = obj.optString("type");
			if(type.equals("pushOp")){
				IStringifiable item = destringifyItem(obj.getString("item"));
				return new PushOp(item, nonce);
			}
			if(type.equals("popOp")){
				return new PopOp(nonce);
			}
			else{
				return new NullOp(nonce);
			}
		}
		catch(JSONException e){
			return null;
		}
	}

}

class StringItem implements IStringifiable{

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
}


class ListState implements IPropState{
	private Vector<IStringifiable> items;

	public ListState(Vector<IStringifiable> items){
		this.items = items;
	}

	public ListState(){
		this(new Vector<IStringifiable>());
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
			for(IStringifiable item : items){
				a.put(item.stringify());
			}
			obj.put("type", "ListState");
			obj.put("items", a);
		}catch(JSONException e){}
		return obj.toString();
	}

	public IPropState copy(){
		return new ListState(items);
	}

	public void push(IStringifiable item){
		items.add(item);
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
	private long nonce;
	private IStringifiable item;

	public PushOp(IStringifiable item, long nonce){
		this.nonce = nonce;
		this.item = item;
	}

	public PushOp(IStringifiable item){
		this(item, (new Random()).nextLong());
	}

	public ListState applyTo(ListState s){
		ListState newS = (ListState)s.copy();
		newS.push(item);
		return newS;
	}

	public long getNonce(){ 
		return this.nonce; 
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "pushOp");
			obj.put("nonce", nonce);
			obj.put("item", item.stringify());
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}


class PopOp implements IPropStateOperation{
	private long nonce;

	public PopOp(long nonce){
		this.nonce = nonce;
	}

	public PopOp(){
		this((new Random()).nextLong());
	}

	public ListState applyTo(ListState s){
		ListState newS = (ListState)s.copy();
		newS.pop();
		return newS;
	}

	public long getNonce(){ 
		return this.nonce; 
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "popOp");
			obj.put("nonce", nonce);
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}
