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
	}

	public LispObject car(){ return car; }

	public LispList cdr(){ return cdr; }

	public LispObject eval(Lisp state) throws LispException{
		if(car instanceof LispSymbol){
			LispObject head = state.lookup((LispSymbol)car);
			if(head instanceof LispSpecial){
				return ((LispSpecial)head).apply(cdr, state);
			}
			else if(head instanceof LispFunc){
				return ((LispFunc)head).apply(cdr, state);
			}
			else{
				throw new LispException("Tried to apply non-function: " + head + " named " + car);
			}
		}
		else{
			throw new LispException("Tried to resolve non-symbol to function: " + car);
		}
	}
}