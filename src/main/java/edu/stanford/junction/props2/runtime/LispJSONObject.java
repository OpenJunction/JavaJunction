package edu.stanford.junction.props2.runtime;

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

import java.util.*;
import java.io.*;
import org.json.*;

public class LispJSONObject extends LispObject{

	public JSONObject obj;

	public LispJSONObject(JSONObject obj){
		this.obj = obj;
	}

	public LispJSONObject(){
		this.obj = new JSONObject();
	}

	public LispObject eval(Lisp state) throws LispException{
		return this;
	}

	public LispObject get(LispString str) throws LispException{
		return wrap(obj.opt(str.value));
	}

	public LispObject put(LispString str, LispObject value) throws LispException{
		try{
			obj.put(str.value, unwrap(value));
		}
		catch(JSONException e){
			throw new LispException(e.toString());
		}
		return this;
	}

	public LispObject del(LispString str) throws LispException{
		obj.remove(str.value);
		return this;
	}

	public static LispObject wrap(Object obj) throws LispException{
		if(obj instanceof JSONObject){
			return new LispJSONObject((JSONObject)obj);
		}
		else if(obj instanceof Number){
			return new LispNumber((Number)obj);
		}
		else if(obj instanceof String){
			return new LispString((String)obj);
		}
		else{
			throw new LispException("Failed to wrap " + obj);
		}
	}

	public static Object unwrap(LispObject obj) throws LispException{
		if(obj instanceof LispJSONObject){
			return ((LispJSONObject)obj).obj;
		}
		else if(obj instanceof LispNumber){
			return ((LispNumber)obj).value;
		}
		else if(obj instanceof LispString){
			return ((LispString)obj).value;
		}
		else{
			throw new LispException("Failed to unwrap " + obj);
		}
	}

}
