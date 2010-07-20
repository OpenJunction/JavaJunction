package edu.stanford.junction.props.sample;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
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
		this(propName, propReplicaName, new StringListItemBuilder());
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
		ListOp s1 = (ListOp)o1;
		ListOp s2 = (ListOp)o2;
		if(s1.getItem().equals(s2.getItem())){
			if(s1 instanceof AddOp && s2 instanceof AddOp){
				// No problem, Set semantics take care of everything.
				return s1;
			}
			else if(s1 instanceof DeleteOp && s2 instanceof DeleteOp){
				// No problem, just delete it..
				return s1;
			}
			else if(s1 instanceof AddOp && s2 instanceof DeleteOp){
				// Delete takes precedence..
				return s2;
			}
			else if(s1 instanceof DeleteOp && s2 instanceof AddOp){
				// Delete takes precedence..
				return s1;
			}
			else{
				throw new UnexpectedOpPairException(o1,o2);
			}
		}
		else{
			// Different items. No conflict possible. Choose either op.
			return s2;
		}
	}

	public void add(IListItem item){
		addOperation(new AddOp(item));
	}

	public void delete(IListItem item){
		addOperation(new DeleteOp(item));
	}

	public List items(){
		ListState s = (ListState)getState();
		return s.unmodifiableList();
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
			add(new StringListItem(words.get(rng.nextInt(words.size()))));
		}
		else{
			Iterator it = items().iterator();
			if(it.hasNext()){
				delete((IListItem)it.next());
			}
		}
	}

	protected IPropState destringifyState(String s){
		try{	   
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("ListState")){
				JSONArray a = obj.getJSONArray("items");
				List<IListItem> items = new ArrayList<IListItem>();
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
			if(type.equals("addOp")){
				IListItem item = builder.destringify(obj.getString("item"));
				return new AddOp(item);
			}
			else if(type.equals("deleteOp")){
				IListItem item = builder.destringify(obj.getString("item"));
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

	public static interface IListItemBuilder{
		IListItem destringify(String s);
	}

	public interface IListItem extends IStringifiable{
		IListItem copy();
	}


	abstract class ListOp implements IPropStateOperation{
		protected ListProp.IListItem item;

		public ListOp(ListProp.IListItem item){
			this.item = item;
		}

		public ListProp.IListItem getItem(){
			return item;
		}

		abstract public String stringify();

		abstract public ListState applyTo(ListState s);
	}

	class AddOp extends ListOp{

		public AddOp(ListProp.IListItem item){
			super(item);
		}

		public ListState applyTo(ListState s){
			ListState newS = (ListState)s.copy();
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

	class DeleteOp extends ListOp{

		public DeleteOp(ListProp.IListItem item){
			super(item);
		}

		public ListState applyTo(ListState s){
			ListState newS = (ListState)s.copy();
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


}


class StringListItemBuilder implements ListProp.IListItemBuilder{
	public ListProp.IListItem destringify(String s){
		try{
			JSONObject obj = new JSONObject(s);
			return new StringListItem(obj.optString("text"));
		}
		catch(JSONException e){
			return new StringListItem("");
		}		
	}
}

class StringListItem implements ListProp.IListItem{

	public String value;

	public StringListItem(String value){
		this.value = value;
	}

	public String stringify(){ 
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "StringListItem");
			obj.put("text", value);
		}catch(JSONException e){}
		return obj.toString();
	}

	public int hashCode(){
		return value.hashCode();
	}

	public boolean equals(Object obj) {
		if(obj instanceof ListProp.IListItem){
			return ((StringListItem)obj).value.equals(value);
		}
		return false;
	}

	public String toString(){ 
		return "\"" + value + "\"";
	}

	public ListProp.IListItem copy(){
		return new StringListItem(value);
	}

}


class ListState implements IPropState{
	private List<ListProp.IListItem> items;

	public ListState(List<ListProp.IListItem> items){
		this.items = new ArrayList(items);
	}

	public ListState(){
		this(new ArrayList<ListProp.IListItem>());
	}

	public IPropState applyOperation(IPropStateOperation operation){
		if(operation instanceof ListProp.ListOp){
			return ((ListProp.ListOp)operation).applyTo(this);
		}
		else{
			return this;
		}
	}

	public List unmodifiableList(){
		return Collections.unmodifiableList(items);
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
		ListState s = new ListState();
		for(ListProp.IListItem ea : items){
			s.add(ea.copy());
		}
		return s;
	}

	public void add(ListProp.IListItem item){
		items.add(item);
	}

	public void delete(ListProp.IListItem item){
		items.remove(item);
	}

	public String toString(){
		return items.toString();
	}
}

