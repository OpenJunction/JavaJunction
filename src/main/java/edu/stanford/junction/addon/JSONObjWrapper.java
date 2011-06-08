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


package edu.stanford.junction.addon;
import org.json.*;
import java.util.*;
import java.util.zip.*;
import org.jivesoftware.smack.util.Base64;
import java.io.*;

/** 
 * A wrapper for JSONObjects that provides hashCode and equals
 * methods based on the underlying JSONObject's 'id' property.
 * 
 * Useful when you want a HashSet/HashMap of JSONObjects, and 
 * each object has a unique 'id'.
 *
 * Also provides deep copy and compression routines.
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
		return (this.opt("id")).hashCode();
	}

	public boolean equals(Object other){
		return other.hashCode() == this.hashCode();
	}

	// Do a deep copy of this object.
	public static JSONObject copyJSONObject(JSONObject obj){
		JSONObject copy = null;
		try{
			copy = new JSONObject(obj.toString());
		}
		catch(JSONException e){}
		return copy;
	}

	public Object clone(){
		return new JSONObjWrapper(copyJSONObject(this));
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
	 * Put a key/boolean pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value A boolean which is the value.
	 * @return this.
	 * @throws JSONException If the key is null.
	 */
	public JSONObject put(String key, boolean value) throws JSONException {
		return self.put(key, value);
	}
 	
 	
	/**
	 * Put a key/double pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value A double which is the value.
	 * @return this.
	 * @throws JSONException If the key is null or if the number is invalid.
	 */
	public JSONObject put(String key, double value) throws JSONException {
		return self.put(key, value);
	}
 	
 	
	/**
	 * Put a key/int pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value An int which is the value.
	 * @return this.
	 * @throws JSONException If the key is null.
	 */
	public JSONObject put(String key, int value) throws JSONException {
		return self.put(key, value);
	}
 	
 	
	/**
	 * Put a key/long pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value A long which is the value.
	 * @return this.
	 * @throws JSONException If the key is null.
	 */
	public JSONObject put(String key, long value) throws JSONException {
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

	public static String compressObj(JSONObject obj){
		String str = obj.toString();
		String result = compressString(str);
		System.out.println("Compressed " + 
						   str.length() + " chars to " + 
						   result.length() + " chars.");
		return result;
	}

	public static JSONObject expandCompressedObj(String compressed){
		String json = decompressString(compressed);
		JSONObject obj = null;
		try{
			obj = new JSONObject(json);
		}
		catch(JSONException e){}
		return  obj;
	}


	private static String decompressString(String str){
		byte[] compressedData = Base64.decode(str);
		// Create the decompressor and give it the data to compress 
		Inflater decompressor = new Inflater(); 
		decompressor.setInput(compressedData); 
		// Create an expandable byte array to hold the decompressed data 
		ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length); 
		// Decompress the data 
		byte[] buf = new byte[1024]; 
		while (!decompressor.finished()) { 
			try { 
				int count = decompressor.inflate(buf); 
				bos.write(buf, 0, count); 
			} catch (DataFormatException e) { 
			} 
		} 
		
		try { 
			bos.close(); 
		} catch (IOException e) { } 

		// Get the decompressed data 
		byte[] decompressedData = bos.toByteArray(); 
		try{
			return new String(decompressedData, "UTF-8");
		}
		catch(UnsupportedEncodingException e){ 
			return new String(decompressedData);
		}
	}


	private static String compressString(String str){
		byte[] input;
		try{
			input = str.getBytes("UTF-8"); 
		}
		catch(UnsupportedEncodingException e){
			input = str.getBytes();
		}
		// Create the compressor with highest level of compression 
		Deflater compressor = new Deflater(); 
		compressor.setLevel(Deflater.BEST_COMPRESSION); 
		// Give the compressor the data to compress 
		compressor.setInput(input); 
		compressor.finish(); 
 
		// Create an expandable byte array to hold the compressed data. 
		// You cannot use an array that's the same size as the orginal because 
		// there is no guarantee that the compressed data will be smaller than 
		// the uncompressed data. 
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length); 
		// Compress the data 
		byte[] buf = new byte[1024];
		while (!compressor.finished()) { 
			int count = compressor.deflate(buf); 
			bos.write(buf, 0, count); 
		} 
		try { 
			bos.close(); 
		} 
		catch (IOException e) { } 
		// Get the compressed data 
		byte[] compressedData = bos.toByteArray(); 
		return Base64.encodeBytes(compressedData);
	}
	
}

