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

abstract public class LispList extends LispObject{
	
	public abstract LispObject car();
	public abstract LispList cdr();

	protected int length = 0;

	public LispObject evalAsBlock(Lisp state) throws LispException{
		LispList forms = this;
		LispObject result = Lisp.nil;
		while(forms != Lisp.nil){
			LispObject form = forms.car();
			result = form.eval(state);
			forms = forms.cdr();
		}
		return result;
	}

	public int length(){
		return length;
	}

	public LispList reverse(){
		LispList ls = Lisp.nil;
		LispList tmp = this;
		while(tmp != Lisp.nil){
			ls = new LispCons(tmp.car(), ls);
			tmp = tmp.cdr();
		}
		return ls;
	}

	public LispObject proplistGet(String str){
		LispList tmp = this;
		while(tmp != Lisp.nil){
			LispObject val = tmp.car();
			if(val instanceof LispString && 
			   ((LispString)val).value.equals(str)){
				return tmp.cdr().car();
			}
			tmp = tmp.cdr();
		}
		return Lisp.nil;
	}
}