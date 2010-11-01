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


package edu.stanford.junction.props2;
import org.json.*;
import java.util.*;
import java.io.*;
import edu.stanford.junction.props2.runtime.*;
import edu.stanford.junction.extra.JSONObjWrapper;

public class StateEngine {

	private Lisp lisp = new Lisp();
	private LispFunc init;
	private Map<String, LispFunc> ops = new HashMap<String, LispFunc>();

	public StateEngine(Reader reader){
		try{
			System.out.println("StateEngine: Loading prop description...");
			LispList desc = (LispList)(lisp.read(new PushbackReader(reader)));
			LispObject fn = desc.proplistGet("initial");
			this.init = (LispFunc)fn.eval(lisp);
			System.out.println("StateEngine: Registered initializer.");
			LispList operations = (LispList)desc.proplistGet("operations");
			LispList tmp = operations;

			while(tmp != Lisp.nil){
				LispString opName = (LispString)(tmp.car().eval(lisp));
				LispFunc op = (LispFunc)(tmp.cdr().car().eval(lisp));
				ops.put(opName.value, op);
				System.out.println("StateEngine: Registered op '" + 
								   opName.value + "'");
				tmp = tmp.cdr().cdr();
			}

		}
		catch(Exception e){
			throw new RuntimeException(e.toString());
		}
	}

	public JSONObject initialState(){
		try{
			return ((LispJSONObject)(init.apply(Lisp.nil, lisp))).obj;
		}
		catch(LispException e){
			throw new RuntimeException(e.toString());
		}
	}

	public JSONObject applyOperation(JSONObject state, JSONObject op){
		try{
			JSONObject newState = JSONObjWrapper.copyJSONObject(state);
			JSONObject newOp = JSONObjWrapper.copyJSONObject(op);
			String type = op.optString("type");
			LispFunc opFunc = ops.get(type);
			if(opFunc == null) 
				throw new RuntimeException(
					"StateEngine: unknown op '" + type + "'"); 
			LispJSONObject result = 
				(LispJSONObject) (opFunc.apply(
									  new LispCons(
										  new LispJSONObject(newState), 
										  new LispCons(
											  new LispJSONObject(newOp), Lisp.nil)),
									  lisp
								  ));
			return result.obj;
		}
		catch(LispException e){
			throw new RuntimeException(e.toString());
		}
	}

}
