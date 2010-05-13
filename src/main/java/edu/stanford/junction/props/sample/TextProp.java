package edu.stanford.junction.props.sample;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Random;
import edu.stanford.junction.props.Prop;
import edu.stanford.junction.props.IPropState;
import edu.stanford.junction.props.IPropStateOperation;
import edu.stanford.junction.props.NullOp;


public class TextProp extends Prop {

	public TextProp(String propName, String propReplicaName){
		super(propName, new TextState("hello out there"), propReplicaName);
	}

	public void insertChar(int index, String charact){
		addOperation(new InsertCharOp(index, charact));
	}

	public void insertRandomChar(){
		Random rng = new Random();
		String alphabet = " abcdefghijklmnopqrstuvwxyz\n";
		int maxLen = ((TextState)getState()).getText().length();
		InsertCharOp op = new InsertCharOp(
			rng.nextInt(maxLen), 
			alphabet.charAt(rng.nextInt(alphabet.length())));
		addOperation(op);
	}

	protected IPropState destringifyState(String s){
		try{
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("TextState")){
				return new TextState(obj.optString("text"));
			}
			else{
				return null;
			}
		}
		catch(JSONException e){
			return null;
		}
	}

	protected IPropStateOperation destringifyOperation(String s){
		try{
			JSONObject obj = new JSONObject(s);
			String type = obj.optString("type");
			if(type.equals("insertChar")){
				int index = obj.optInt("index");
				String charact = obj.optString("char");
				return new InsertCharOp(index, charact);
			}
			else if(type.equals("null")){
				return new NullOp();
			}
			else{
				return null;
			}
		}
		catch(JSONException e){
			return null;
		}
	}

}


class TextState implements IPropState{
	private String text;

	public TextState(){
		text = "";
	}

	public TextState(String txt){
		text = txt;
	}

	public IPropState applyOperation(IPropStateOperation operation){
		if(operation instanceof InsertCharOp){
			InsertCharOp op = (InsertCharOp)operation;
			return op.applyTo(this);
		}
		else{
			TextState newState = (TextState)this.copy();
			return newState;
		}
	}

	public IPropStateOperation nullOperation(){
		return new NullOp();
	}

	public String hash(){
		return text;
	}

	public String stringify(){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "TextState");
			obj.put("text", text);
		}catch(JSONException e){}
		return obj.toString();
	}

	public IPropState copy(){
		return new TextState(text);
	}

	public void setText(String txt){
		text = txt;
	}

	public String getText(){
		return text;
	}

	public String toString(){
		return text;
	}
}

class InsertCharOp implements IPropStateOperation{
	private int i;
	private String c;

	public InsertCharOp(int index, String charact){
		this.i = index;
		this.c = charact;
	}

	public InsertCharOp(int index, char charact){
		this(index, String.valueOf(charact));
	}

	public TextState applyTo(TextState s){
		String newText = s.getText().substring(0,i) + c + s.getText().substring(i + 1);
		return new TextState(newText);
	}

	private JSONObject toJSONObject(){
		JSONObject obj = new JSONObject();
		try{
			obj.put("type", "insertChar");
			obj.put("index", this.i);
			obj.put("char", this.c);
		}catch(JSONException e){}
		return obj;
	}

	public String stringify(){
		return toJSONObject().toString();
	}
}

