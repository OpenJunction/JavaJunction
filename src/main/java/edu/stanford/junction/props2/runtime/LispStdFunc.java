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

public class LispStdFunc extends LispFunc{

	final LispList params;
	final LispList body;

	public LispStdFunc(LispList params, LispList body){
		this.params = params;
		this.body = body;
	}

	public LispObject eval(Lisp state) throws LispException{
		return this;
	}

	public LispObject apply(LispList args, Lisp state) throws LispException{
		if(args.length() != params.length())
			throw new LispException("Function arity mismatch!");

		Map<LispSymbol, LispObject> toBind = new HashMap<LispSymbol, LispObject>();

		LispList prms = params;
		LispList argz = args;
		while(prms != Lisp.nil){
			LispSymbol param = (LispSymbol)prms.car();
			LispObject arg = argz.car();
			toBind.put(param, arg);
			prms = prms.cdr();
			argz = argz.cdr();
		}

		state.pushBinding(toBind);
		LispObject result = body.evalAsBlock(state);
		state.popBinding();
		return result;
	}

}