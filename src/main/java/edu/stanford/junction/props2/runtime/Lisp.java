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
import java.util.regex.*;
import java.io.*;


public class Lisp{

	public static final LispNil nil = new LispNil();
	public static final LispTruth truth = new LispTruth();


	private Map<String,LispSymbol> symbols = new HashMap<String,LispSymbol>();
	private Stack<Map<LispSymbol,LispObject>> bindings = 
		new Stack<Map<LispSymbol,LispObject>>();

	public static final LispSpecial LET_SPECIAL = new LetSpecial();
	static class LetSpecial extends LispSpecial{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispList binders = (LispList)args.car();
			int bindCount = 0;
			while(binders != nil){
				LispList binder = (LispList)binders.car();
				LispSymbol name = (LispSymbol)binder.car();
				LispObject val = (binder.cdr().car()).eval(state);
				state.pushBinding(name, val);
				bindCount += 1;
				binders = binders.cdr();
			}
			LispList body = args.cdr();
			LispObject result = body.evalAsBlock(state);
			for(int i = 0; i < bindCount; i++) state.popBinding();
			return result;
		}
	}

	public static final LispSpecial IF_SPECIAL = new IfSpecial();
	static class IfSpecial extends LispSpecial{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispObject test = args.car();
			LispObject thenForm = args.cdr().car();
			LispObject elseForm = args.cdr().cdr().car();
			LispObject testResult = test.eval(state);
			if(testResult != nil){
				return thenForm.eval(state);
			}
			else {
				return elseForm.eval(state);
			}
		}
	}

	public static final LispSpecial FN_SPECIAL = new FnSpecial();
	static class FnSpecial extends LispSpecial{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispList params = (LispList)(args.car());
			LispList body = args.cdr();
			return new LispStdFunc(params, body);
		}
	}

	public static final LispFunc PUT_FUNC = new PutFunc();
	static class PutFunc extends LispFunc{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispJSONObject obj = (LispJSONObject)(args.car());
			LispString name = (LispString)(args.cdr().car());
			LispObject val = args.cdr().cdr().car();
			return obj.put(name, val);
		}
	}

	public static final LispFunc GET_FUNC = new GetFunc();
	static class GetFunc extends LispFunc{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispJSONObject obj = (LispJSONObject)(args.car());
			LispString name = (LispString)(args.cdr().car());
			return obj.get(name);
		}
	}

	public static final LispFunc DEL_FUNC = new DelFunc();
	static class DelFunc extends LispFunc{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispJSONObject obj = (LispJSONObject)(args.car());
			LispString name = (LispString)(args.cdr().car());
			return obj.del(name);
		}
	}
	
	public static final LispFunc NEW_OBJ_FUNC = new NewObjFunc();
	static class NewObjFunc extends LispFunc{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			return new LispJSONObject();
		}
	}

	public static final LispFunc PLUS_FUNC = new PlusFunc();
	static class PlusFunc extends LispFunc{
		public LispObject apply(LispList args, Lisp state) throws LispException{
			LispNumber a = (LispNumber)(args.car());
			LispNumber b = (LispNumber)(args.cdr().car());
			return new LispNumber(a.value.doubleValue() + b.value.doubleValue());
		}
	}

	public Lisp(){
		Map<LispSymbol, LispObject> root = new HashMap<LispSymbol, LispObject>();
		root.put(intern("let"), LET_SPECIAL);
		root.put(intern("if"), IF_SPECIAL);
		root.put(intern("fn"), FN_SPECIAL);
		root.put(intern("t"), truth);
		root.put(intern("nil"), nil);
		root.put(intern("put"), PUT_FUNC);
		root.put(intern("get"), GET_FUNC);
		root.put(intern("del"), DEL_FUNC);
		root.put(intern("new-obj"), NEW_OBJ_FUNC);
		root.put(intern("+"), PLUS_FUNC);
		pushBinding(root);
	}

	public LispSymbol intern(LispSymbol sym){
		if(symbols.containsKey(sym.value)) return symbols.get(sym.value);
		else{
			symbols.put(sym.value, sym);
			return sym;
		}
	}

	public LispSymbol intern(String symName){
		return intern(new LispSymbol(symName));
	}

	public void pushBinding(Map<LispSymbol,LispObject> binding){
		bindings.push(binding);
	}

	public void pushBinding(LispSymbol name, LispObject val){
		Map<LispSymbol,LispObject> binding = new HashMap<LispSymbol,LispObject>();
		binding.put(name, val);
		bindings.push(binding);
	}

	public void popBinding(){
		bindings.pop();
	}

	public LispObject lookup(LispSymbol sym){
		for(int i = bindings.size() - 1; i > -1; i--){
			Map<LispSymbol, LispObject> b = bindings.get(i);
			if(b.containsKey(sym)) return b.get(sym);
		}
		return null;
	}

	public LispObject eval(PushbackReader reader) throws LispException, IOException, ReaderException{
		LispObject form = read(reader);
		return form.eval(this);
	}

	public LispObject read(PushbackReader r) throws IOException, ReaderException{
		return read(r, false);
	}

	private LispObject read(PushbackReader r, boolean readList) throws IOException, ReaderException{
		ArrayList<LispObject> list = new ArrayList<LispObject>();
		LispObject result = null;
		for(; ;){
			int ch = r.read();

			while(Character.isWhitespace(ch))
				ch = r.read();

			if(Character.isDigit(ch)){
				result = readNumber(r, (char)ch);
			}
			else if(ch == '"'){
				result = readString(r);
			}
			else if(isSymStart((char)ch)){
				result = readSymbol(r, (char)ch);
			}
			else if(ch == '('){
				result = read(r, true);
			}
			else if(readList && ch == ')'){
				LispList ls = nil;
				for(int i = list.size() - 1; i > -1; i--){
					ls = new LispCons(list.get(i),ls);
				}
				return ls;
			}
			else if((int)ch == -1){
				throw new ReaderException("EOF while reading");
			}
			else{
				throw new ReaderException("Unrecognized character '" + ch + "'");
			}

			if(readList){
				list.add(result);
			}
			else{
				return result;
			}

		}
	}

	private boolean isSymStart(char ch){
		return Character.isLetter(ch) || ch == '+' || ch == '-' || 
			ch == '*' || ch == '/';
	}

	private boolean isSymTail(char ch){
		return Character.isLetter(ch) || Character.isDigit(ch) || 
			ch == '+' || ch == '-' || ch == '*' || ch == '/';
	}

	private LispObject readString(PushbackReader r) throws IOException, ReaderException{
		StringBuffer sb = new StringBuffer();
		for( ; ; ){
			int ch = r.read();
			if(ch == '"'){
				return new LispString(sb.toString());
			}
			else if(ch == -1){
				throw new ReaderException("EOF while reading string");
			}
			else sb.append((char)ch); 
		}
	}

	private LispObject readSymbol(PushbackReader r, char initial) throws IOException, ReaderException{
		StringBuffer sb = new StringBuffer();
		sb.append(initial);
		for( ; ; ){
			int ch = r.read();
			if(isSymTail((char)ch)){
				sb.append((char)ch);
			}
			else {
				r.unread(ch);
				break;
			}
		}
		String str = sb.toString();
		return intern(sb.toString());
	}

	private LispObject readNumber(PushbackReader r, char initial) throws IOException, ReaderException{
		StringBuffer sb = new StringBuffer();
		sb.append(initial);
		for( ; ; ){
			int ch = r.read();
			if(Character.isDigit(ch)){
				sb.append((char)ch);
			}
			else {
				r.unread(ch);
				break;
			}
		}
		String str = sb.toString();
		return new LispNumber(Integer.valueOf(str));
	}

}


