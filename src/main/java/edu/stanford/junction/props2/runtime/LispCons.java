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

public class LispCons extends LispList{
	final LispObject car;
	final LispList cdr;

	public LispCons(final LispObject car, final LispList cdr){
		this.car = car;
		this.cdr = cdr;

		int len = 1;
		LispList ls = cdr;
		while(ls != Lisp.nil){
			len++;
			ls = ls.cdr();
		}
		length = len;
	}

	public LispObject car(){ return car; }

	public LispList cdr(){ return cdr; }

	public LispObject eval(Lisp state) throws LispException{
		LispObject head = car.eval(state);
		if(head instanceof LispSpecial){
			return ((LispSpecial)head).apply(cdr, state);
		}
		else if(head instanceof LispFunc){
			LispList args = Lisp.nil;
			LispList argForms = cdr;
			while(argForms != Lisp.nil){
				LispObject arg = argForms.car().eval(state);
				args = new LispCons(arg, args);
				argForms = argForms.cdr();
			}
			return ((LispFunc)head).apply(args.reverse(), state);
		}
		else{
			throw new LispException("Tried to apply non-function: " + head + " named " + car);
		}
	}
}