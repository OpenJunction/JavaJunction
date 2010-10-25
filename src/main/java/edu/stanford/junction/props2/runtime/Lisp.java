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


public class Lisp{

	public static class ReaderException extends Exception{
		public ReaderException(String msg){
			super(msg);
		}
	}

	public static abstract class LispObject{
	}

	public static class LispSymbol extends LispObject{
		final String name;
		public LispSymbol(final String str){
			name = str;
		}
	}

	public static class LispString extends LispObject{
		final String value;
		public LispString(final String str){
			value = str;
		}
	}

	public static class LispNumber extends LispObject{
		final Number value;
		public LispNumber(final Number num){
			value = num;
		}
	}

	public static class LispCons extends LispObject{
		final LispObject car;
		final LispObject cdr;
		public LispCons(final LispObject car, final LispObject cdr){
			this.car = car;
			this.cdr = cdr;
		}
	}

	public static class LispNil extends LispObject{}

	public static final LispObject nil = new LispNil();


//	private Map<String,LispObject> symTable = new HashMap<String,LispObject>();
//	private LispObject cons = new Cons();
//	private LispFunction consFn = new FnCons();
	
	public Lisp(){
//		symTable;
//		symTable.put("cons", consFn);
	}


	public static LispObject read(Reader r) throws IOException, ReaderException{
		for(; ;){
			int ch = r.read();

			while(Character.isWhitespace(ch))
				ch = r.read();

			if(Character.isDigit(ch)){
				LispObject n = readNumber(r, (char)ch);
				return n;
			}
			else if(ch == '"'){
				return readString(r);
			}
			else if(Character.isLetter(ch)){
				return readSymbol(r, (char)ch);
			}
			else if(ch == '('){
				return new LispCons(read(r), readTail(r));
			}
			else if(ch == ')'){
				return nil;
			}
			else if((int)ch == -1){
				throw new ReaderException("EOF while reading");
			}
			else{
				throw new ReaderException("Unrecognized character '" + ch + "'");
			}
		}
	}

	private static LispObject readTail(Reader r) throws IOException, ReaderException{
		LispObject next = read(r);
		if(next == nil) return nil;
		else return new LispCons(next, readTail(r));
	}

	private static LispObject readString(Reader r) throws IOException, ReaderException{
		StringBuffer sb = new StringBuffer();
		for( ; ; ){
			int ch = r.read();
			if(ch == -1 || ch == '"'){
				return new LispString(sb.toString());
			}
			else sb.append(ch); 
		}
	}

	private static LispObject readSymbol(Reader r, char initial) throws IOException, ReaderException{
		StringBuffer sb = new StringBuffer();
		sb.append(initial);
		for( ; ; ){
			int ch = r.read();
			if(ch == -1 || Character.isWhitespace(ch)){
				return new LispSymbol(sb.toString());
			}
			else sb.append(ch);
		}
	}

	private static LispObject readNumber(Reader r, char initial) throws IOException, ReaderException{
		StringBuffer sb = new StringBuffer();
		sb.append(initial);
		for( ; ; ){
			int ch = r.read();
			if(ch == -1 || Character.isWhitespace(ch)){
				String str = sb.toString();
				return new LispNumber(Integer.valueOf(str));
			}
			else sb.append(ch);
		}
	}

}


