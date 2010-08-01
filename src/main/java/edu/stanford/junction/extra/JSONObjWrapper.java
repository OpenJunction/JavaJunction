package edu.stanford.junction.extra;
import org.json.*;
import java.util.*;


/** 
 * A wrapper for JSONObjects that provides hashCode and equals
 * methods based on the underlying JSONObject's 'id' property.
 * 
 * Useful when you want a HashSet/HashMap of JSONObjects, and 
 * each object has a unique 'id'.
 *
 */
public class JSONObjWrapper extends JSONObject{
	

	private JSONObject self;


	public JSONObjWrapper(JSONObject obj){
		this.self = obj;
	}

	public JSONObject getRaw(){
		return self;
	}

	public int hashCode(){
		return this.optInt("id");
	}

	public boolean equals(Object other){
		return other.hashCode() == this.hashCode();
	}

	public Object clone(){
		JSONObject copy = new JSONObject();
		try{
			Iterator it = this.keys();
			while(it.hasNext()){
				String key = (String)it.next();
				copy.put(key, this.get(key));
			}
		}
		catch(JSONException e){}
		return new JSONObjWrapper(copy);
	}

	/** 
	 * Get the value object associated with a key.
	 *
	 * @param key   A key string.
	 * @return      The object associated with the key.
	 * @throws   JSONException if the key is not found.
	 */
	public Object get(String key) throws JSONException {
		return self.get(key);
	}
	
	
	/**
	 * Determine if the JSONObject contains a specific key.
	 * @param key   A key string.
	 * @return      true if the key exists in the JSONObject.
	 */
	public boolean has(String key) {
		return self.has(key);
	}
	
	
	/**
	 * Get an enumeration of the keys of the JSONObject.
	 *
	 * @return An iterator of the keys.
	 */
	public Iterator keys() {
		return self.keys();
	}
	
	
	/**
	 * Get the number of keys stored in the JSONObject.
	 *
	 * @return The number of keys in the JSONObject.
	 */
	public int length() {
		return self.length();
	}
	
	
	
	/**
	 * Get an optional value associated with a key.
	 * @param key   A key string.
	 * @return      An object which is the value, or null if there is no value.
	 */
	public Object opt(String key) {
		return self.opt(key);
	}
	
	
	
	/**
	 * Put a key/value pair in the JSONObject. If the value is null,
	 * then the key will be removed from the JSONObject if it is present.
	 * @param key   A key string.
	 * @param value An object which is the value. It should be of one of these
	 *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
	 *  or the JSONObject.NULL object.
	 * @return this.
	 * @throws JSONException If the value is non-finite number
	 *  or if the key is null.
	 */
	public JSONObject put(String key, Object value) throws JSONException{
		return self.put(key, value);
	}
	
	
	/**
	 * Remove a name and its value, if present.
	 * @param key The name to be removed.
	 * @return The value that was associated with the name,
	 * or null if there was no value.
	 */
	public Object remove(String key) {
		return self.remove(key);
	}

	public String toString() {
		return self.toString();
	}

	public String toString(int indentFactor) throws JSONException {
		return self.toString(indentFactor);
	}
	
}

