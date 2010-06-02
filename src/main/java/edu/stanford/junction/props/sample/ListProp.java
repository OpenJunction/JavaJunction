package edu.stanford.junction.props.sample;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import edu.stanford.junction.props.*;


public class ListProp extends Prop {

	IListItemBuilder builder;

	public ListProp(String propName, String propReplicaName, IListItemBuilder builder, List<IListItem> items){
		super(propName, new ListState(items), propReplicaName);
		this.builder = builder;
	}

	public ListProp(String propName, String propReplicaName, IListItemBuilder builder){
		this(propName, propReplicaName, builder, new ArrayList<IListItem>());
	}

	public ListProp(String propName, String propReplicaName){
		this(propName, propReplicaName, new StringItemBuilder());
	}

	/**
	 * Assume o1 and o2 operate on the same state s.
	 * 
	 * Intent Preservation:
	 * transposeForward(o1,o2) is a new operation, defined on the state resulting from the execution of o1, 
	 * and realizing the same intention as op2.
	 * 
	 * Convergence:
	 * It must hold that o1*transposeForward(o1,o2) = o2*transposeForward(o2,o1).
	 *
	 * (where oi*oj denotes the execution of oi followed by the execution of oj)
	 * 
	 */
	protected IPropStateOperation transposeForward(IPropStateOperation o1, IPropStateOperation o2) throws UnexpectedOpPairException{
		ListOp l1 = (ListOp)o1;
		ListOp l2 = (ListOp)o2;
		return l2;
	}

	public void push(IListItem item){
		addOperation(new PushOp(item));
	}

	public void pop(){
		addOperation(new PopOp());
	}

	public void pushRandom(){
		ArrayList<String> words = new ArrayList<String>();
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
				ArrayList<IListItem> items = new ArrayList<IListItem>();
				for(int i = 0; i < a.length(); i++){
					IListItem item = builder.destringify(a.getString(i));
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

	protected IPropStateOperation destringifyOperation(String s){
		try{
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("pushOp")){
				IListItem item = builder.destringify(obj.getString("item"));
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

	public static interface IListItemBuilder{
		IListItem destringify(String s);
	}

	public interface IListItem extends IStringifiable{
		IListItem copy();
	}

}


class StringItemBuilder implements ListProp.IListItemBuilder{
	public ListProp.IListItem destringify(String s){
		try{
			JSONObject obj = new JSONObject(s);
			return new StringItem(obj.optString("text"));
		}
		catch(JSONException e){
			return new StringItem("");
		}		
	}
}

class StringItem implements ListProp.IListItem{

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

	public ListProp.IListItem copy(){
		return new StringItem(value);
	}

}


class ListState implements IPropState{
	private List<ListProp.IListItem> items;

	public ListState(List<ListProp.IListItem> items){
		this.items = new ArrayList<ListProp.IListItem>(items);
	}

	public ListState(){
		this(new ArrayList<ListProp.IListItem>());
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
			for(ListProp.IListItem item : items){
				a.put(item.stringify());
			}
			obj.put("type", "ListState");
			obj.put("items", a);
		}catch(JSONException e){}
		return obj.toString();
	}

	public IPropState copy(){
		ListState l = new ListState();
		for(ListProp.IListItem ea : items){
			l.push(ea.copy());
		}
		return l;
	}

	public void push(ListProp.IListItem item){
		items.add(item);
	}

	public void insert(ListProp.IListItem item, int index){
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

abstract class ListOp implements IPropStateOperation{
	abstract public String stringify();
}

class PushOp extends ListOp{
	private ListProp.IListItem item;

	public PushOp(ListProp.IListItem item){
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

class PopOp extends ListOp{

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



class MoveOp extends ListOp {

	private int fromIndex;
	private int toIndex;

	public MoveOp(int fromIndex, int toIndex){
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	public ListState applyTo(ListState s){
		ListState newS = (ListState)s.copy();
//		newS.moveItem(fromIndex, toIndex);
		return newS;
	}

	public String stringify(){
		try{
			JSONObject obj = new JSONObject();
			obj.put("type", "pushOp");
			obj.put("fromIndex", fromIndex);
			obj.put("toIndex", toIndex);
			return obj.toString();
		}catch(JSONException e){}
		return "";
	}
}