/*
 * TinyJS
 *
 * A single-file Javascript-alike engine
 *
 * Authored By Gordon Williams <gw@pur3.co.uk>
 *
 * Copyright (C) 2009 Pur3 Ltd
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package edu.stanford.junction.props2.runtime;

import java.util.*;

class CTinyJS {

    public static final int TINYJS_LOOP_MAX_ITERATIONS = 8192;
    public static final String TINYJS_RETURN_VAR = "return";
    public static final String TINYJS_PROTOTYPE_CLASS = "prototype";
    public static final String TINYJS_TEMP_NAME = "";
    public static final String TINYJS_BLANK_DATA = "";


    public CScriptVar root;   /// root of symbol table
    private CScriptLex l;             /// current lexer
    private ArrayList<CScriptVar> scopes; /// stack of scopes when parsing
    private CScriptVar stringClass; /// Built in string class
    private CScriptVar objectClass; /// Built in object class
    private CScriptVar arrayClass; /// Built in array class


	public CTinyJS(){
		l = null;
		root = (new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT)).ref();
		// Add built-in classes
		stringClass = (new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT)).ref();
		arrayClass = (new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT)).ref();
		objectClass = (new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT)).ref();
		root.addChild("String", stringClass);
		root.addChild("Array", arrayClass);
		root.addChild("Object", objectClass);
	}

	public void delete(){
		scopes.clear();
		stringClass.unref();
		arrayClass.unref();
		objectClass.unref();
		root.unref();
	}

	public static CScriptVarLink createLink(CScriptVarLink link, CScriptVar var){
		if ((link == null) || link.owned) {
			return new CScriptVarLink(var, TINYJS_TEMP_NAME);
		}
		else{
			link.replaceWith(var); 
			return link;
		}
	}

	public static CScriptVar back(ArrayList<CScriptVar> scopes){
		if(scopes.isEmpty()) return null;
		else return scopes.get(scopes.size() - 1);
	}


    public void execute(String code){
		CScriptLex oldLex = l;
		ArrayList<CScriptVar> oldScopes = new ArrayList<CScriptVar>(scopes);
		l = new CScriptLex(code);
		scopes.clear();
		scopes.add(root);
		try {
			boolean execute = true;
			while (l.tk != 0) statement(execute);
		} catch (CScriptException e) {
			String msg = "Error " + e.text + " at " + l.getPosition(-1);
			l.delete();
			l = oldLex;
			throw new CScriptException(msg);
		}
		l.delete();
		l = oldLex;
		scopes = oldScopes;
	}

    /** Evaluate the given code and return a link to a javascript object,
     * useful for (dangerous) JSON parsing. If nothing to return, will return
     * 'undefined' variable type. CScriptVarLink is returned as this will
     * automatically unref the result as it goes out of scope. If you want to
     * keep it, you must use ref() and unref() */
    CScriptVarLink evaluateComplex(String code){
		CScriptLex oldLex = l;
		ArrayList<CScriptVar> oldScopes = new ArrayList<CScriptVar>(scopes);
		l = new CScriptLex(code);
		scopes.clear();
		scopes.add(root);
		CScriptVarLink v = null;
		try {
			boolean execute = true;
			do {
				v = base(execute);
				if (l.tk != CScriptLex.LEX_EOF) l.match(';');
			} while (l.tk != CScriptLex.LEX_EOF);
		} catch (CScriptException e) {
			String msg = "Error " + e.text + " at " + l.getPosition(-1);
			l.delete();
			l = oldLex;
			throw new CScriptException(msg);
		}
		l.delete();
		l = oldLex;
		scopes = oldScopes;

		if (v != null) {
			CScriptVarLink r = new CScriptVarLink(v);
			return r;
		}

		// return undefined...
		return new CScriptVarLink(new CScriptVar(), TINYJS_TEMP_NAME);

	}

    /** Evaluate the given code and return a string. If nothing to return, will return
     * 'undefined' */
    public String evaluate(String code){
		return evaluateComplex(code).var.getString();
	}


    /// add a native function to be called from TinyJS
    /** example:
		\code
		void scRandInt(CScriptVar *c, void *userdata) { ... }
		tinyJS.addNative("function randInt(min, max)", scRandInt, 0);
		\endcode

		or

		\code
		void scSubstring(CScriptVar *c, void *userdata) { ... }
		tinyJS.addNative("function String.substring(lo, hi)", scSubstring, 0);
		\endcode
    */
    public void addNative(String funcDesc, JSCallback ptr, Object userdata){
		CScriptLex oldLex = l;
		l = new CScriptLex(funcDesc);

		CScriptVar base = root;

		l.match(CScriptLex.LEX_R_FUNCTION);
		String funcName = l.tkStr;
		l.match(CScriptLex.LEX_ID);
		/* Check for dots, we might want to do something like function String.substring ... */
		while (l.tk == '.') {
			l.match('.');
			CScriptVarLink link = base.findChild(funcName);
			// if it doesn't exist, make an object class
			if (link == null) link = base.addChild(funcName, new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT));
			base = link.var;
			funcName = l.tkStr;
			l.match(CScriptLex.LEX_ID);
		}

		CScriptVar funcVar = new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_FUNCTION | CScriptVar.SCRIPTVAR_NATIVE);
		funcVar.setCallback(ptr, userdata);
		parseFunctionArguments(funcVar);
		l.delete();
		l = oldLex;

		base.addChild(funcName, funcVar);
	}

    /// Get the value of the given variable, or return 0
    public String getVariable(String path){
		// traverse path
		int prevIdx = 0;
		int thisIdx = path.indexOf('.');
		if (thisIdx == -1) thisIdx = path.length();
		CScriptVar var = root;
		while ((var!=null) && prevIdx < path.length()) {
			String el = path.substring(prevIdx, thisIdx - prevIdx);
			CScriptVarLink varl = var.findChild(el);
			var = (varl != null) ? varl.var : null;
			prevIdx = thisIdx+1;
			thisIdx = path.indexOf('.', prevIdx);
			if (thisIdx == -1) thisIdx = path.length();
		}

		// return result
		if (var != null)
			return var.getString();
		else
			return null;
	}

    /// Send all variables to stdout
    public void trace() {
		root.trace("", "");
	}

    // parsing - in order of precedence
    private CScriptVarLink factor(boolean execute){
		if (l.tk=='(') {
			l.match('(');
			CScriptVarLink a = base(execute);
			l.match(')');
			return a;
		}
		if (l.tk==CScriptLex.LEX_R_TRUE) {
			l.match(CScriptLex.LEX_R_TRUE);
			return new CScriptVarLink(new CScriptVar(1), TINYJS_TEMP_NAME);
		}
		if (l.tk==CScriptLex.LEX_R_FALSE) {
			l.match(CScriptLex.LEX_R_FALSE);
			return new CScriptVarLink(new CScriptVar(0), TINYJS_TEMP_NAME);
		}
		if (l.tk==CScriptLex.LEX_R_NULL) {
			l.match(CScriptLex.LEX_R_NULL);
			return new CScriptVarLink(new CScriptVar(TINYJS_BLANK_DATA,CScriptVar.SCRIPTVAR_NULL), TINYJS_TEMP_NAME);
		}
		if (l.tk==CScriptLex.LEX_R_UNDEFINED) {
			l.match(CScriptLex.LEX_R_UNDEFINED);
			return new CScriptVarLink(new CScriptVar(TINYJS_BLANK_DATA,CScriptVar.SCRIPTVAR_UNDEFINED), TINYJS_TEMP_NAME);
		}
		if (l.tk==CScriptLex.LEX_ID) {
			CScriptVarLink a = execute ? findInScopes(l.tkStr) : new CScriptVarLink(new CScriptVar(), TINYJS_TEMP_NAME);
			//printf("0x%08X for %s at %s\n", (unsigned int)a, l.tkStr.c_str(), l.getPosition().c_str());
			/* The parent if we're executing a method call */
			CScriptVar parent = null;

			if (execute && (a == null)) {
				/* Variable doesn't exist! JavaScript says we should create it
				 * (we won't add it here. This is done in the assignment operator)*/
				a = new CScriptVarLink(new CScriptVar(), l.tkStr);
			}
			l.match(CScriptLex.LEX_ID);
			while (l.tk=='(' || l.tk=='.' || l.tk=='[') {
				if (l.tk=='(') { // ------------------------------------- Function Call
					if (execute) {
						if (!a.var.isFunction()) {
							String errorMsg = "Expecting '";
							errorMsg = errorMsg + a.name + "' to be a function";
							throw new CScriptException(errorMsg);
						}
						l.match('(');
						// create a new symbol table entry for execution of this function
						CScriptVar functionRoot = new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_FUNCTION);
						if (parent != null)
							functionRoot.addChildNoDup("this", parent);
						// grab in all parameters
						CScriptVarLink v = a.var.firstChild;
						while (v != null) {
							CScriptVarLink value = base(execute);
							if (execute) {
								if (value.var.isBasic()) {
									// pass by value
									functionRoot.addChild(v.name, value.var.deepCopy());
								} else {
									// pass by reference
									functionRoot.addChild(v.name, value.var);
								}
							}
							if (l.tk!=')') l.match(',');
							v = v.nextSibling;
						}
						l.match(')');
						// setup a return variable
						CScriptVarLink returnVar = null;
						// execute function!
						// add the function's execute space to the symbol table so we can recurse
						CScriptVarLink returnVarLink = functionRoot.addChild(TINYJS_RETURN_VAR, null);
						scopes.add(functionRoot);

						if (a.var.isNative()) {
							assert a.var.getJSCallback() != null;
							a.var.getJSCallback().apply(functionRoot, a.var.jsCallbackUserData);
						} else {
							/* we just want to execute the block, but something could
							 * have messed up and left us with the wrong ScriptLex, so
							 * we want to be careful here... */
							CScriptException exception = null;
							CScriptLex oldLex = l;
							CScriptLex newLex = new CScriptLex(a.var.getString());
							l = newLex;
							try {
								block(execute);
								// because return will probably have called this, and set execute to false
								execute = true;
							} catch (CScriptException e) {
								exception = e;
							}
							newLex.delete();
							l = oldLex;

							if (exception != null)
								throw exception;
						}

						scopes.remove(scopes.size() - 1);
						/* get the real return var before we remove it from our function */
						returnVar = new CScriptVarLink(returnVarLink.var, TINYJS_TEMP_NAME);
						functionRoot.removeLink(returnVarLink);
						functionRoot.delete();
						if (returnVar != null)
							a = returnVar;
						else
							a = new CScriptVarLink(new CScriptVar(), TINYJS_TEMP_NAME);
					} else {
						// function, but not executing - just parse args and be done
						l.match('(');
						while (l.tk != ')') {
							CScriptVarLink value = base(execute);
							if (l.tk!=')') l.match(',');
						}
						l.match(')');
						if (l.tk == '{') {
							block(execute);
						}
					}
				} else if (l.tk == '.') { // ------------------------------------- Record Access
					l.match('.');
					if (execute) {
						String name = l.tkStr;
						CScriptVarLink child = a.var.findChild(name);
						if (child == null) child = findInParentClasses(a.var, name);
						if (child == null) {
							/* if we haven't found this defined yet, use the built-in
							   'length' properly */
							if (a.var.isArray() && name == "length") {
								int l = a.var.getArrayLength();
								child = new CScriptVarLink(new CScriptVar(l), TINYJS_TEMP_NAME);
							} else if (a.var.isString() && name == "length") {
								int l = a.var.getString().length();
								child = new CScriptVarLink(new CScriptVar(l), TINYJS_TEMP_NAME);
							} else {
								child = a.var.addChild(name, null);
							}
						}
						parent = a.var;
						a = child;
					}
					l.match(CScriptLex.LEX_ID);
				} else if (l.tk == '[') { // ------------------------------------- Array Access
					l.match('[');
					CScriptVarLink index = expression(execute);
					l.match(']');
					if (execute) {
						CScriptVarLink child = a.var.findChildOrCreate(
							index.var.getString(), CScriptVar.SCRIPTVAR_UNDEFINED);
						parent = a.var;
						a = child;
					}
				} else assert false;
			}
			return a;
		}
		if (l.tk==CScriptLex.LEX_INT || l.tk==CScriptLex.LEX_FLOAT) {
			CScriptVar a = new CScriptVar(l.tkStr,
										  ((l.tk==CScriptLex.LEX_INT) ? CScriptVar.SCRIPTVAR_INTEGER : CScriptVar.SCRIPTVAR_DOUBLE));
			l.match(l.tk);
			return new CScriptVarLink(a, TINYJS_TEMP_NAME);
		}
		if (l.tk==CScriptLex.LEX_STR) {
			CScriptVar a = new CScriptVar(l.tkStr, CScriptVar.SCRIPTVAR_STRING);
			l.match(CScriptLex.LEX_STR);
			return new CScriptVarLink(a, TINYJS_TEMP_NAME);
		}
		if (l.tk=='{') {
			CScriptVar contents = new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT);
			/* JSON-style object definition */
			l.match('{');
			while (l.tk != '}') {
				String id = l.tkStr;
				// we only allow strings or IDs on the left hand side of an initialisation
				if (l.tk==CScriptLex.LEX_STR) l.match(CScriptLex.LEX_STR);
				else l.match(CScriptLex.LEX_ID);
				l.match(':');
				if (execute) {
					CScriptVarLink a = base(execute);
					contents.addChild(id, a.var);
				}
				// no need to clean here, as it will definitely be used
				if (l.tk != '}') l.match(',');
			}

			l.match('}');
			return new CScriptVarLink(contents, TINYJS_TEMP_NAME);
		}
		if (l.tk=='[') {
			CScriptVar contents = new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_ARRAY);
			/* JSON-style array */
			l.match('[');
			int idx = 0;
			while (l.tk != ']') {
				if (execute) {
					CScriptVarLink a = base(execute);
					contents.addChild(String.valueOf(idx), a.var);
				}
				// no need to clean here, as it will definitely be used
				if (l.tk != ']') l.match(',');
				idx++;
			}
			l.match(']');
			return new CScriptVarLink(contents, TINYJS_TEMP_NAME);
		}
		if (l.tk==CScriptLex.LEX_R_FUNCTION) {
			CScriptVarLink funcVar = parseFunctionDefinition();
			if (funcVar.name != TINYJS_TEMP_NAME)
				System.err.println("Functions not defined at statement-level are not meant to have a name");
			return funcVar;
		}
		if (l.tk==CScriptLex.LEX_R_NEW) {
			// new . create a new object
			l.match(CScriptLex.LEX_R_NEW);
			String className = l.tkStr;
			if (execute) {
				CScriptVarLink objClass = findInScopes(className);
				if (objClass == null) {
					System.err.println(className + " is not a valid class name");
					return new CScriptVarLink(new CScriptVar(), TINYJS_TEMP_NAME);
				}
				l.match(CScriptLex.LEX_ID);
				CScriptVar obj = new CScriptVar(TINYJS_BLANK_DATA, CScriptVar.SCRIPTVAR_OBJECT);
				obj.addChild(TINYJS_PROTOTYPE_CLASS, objClass.var);
				if (l.tk == '(') {
					l.match('(');
					l.match(')');
				}
				// TODO: Object constructors
				return new CScriptVarLink(obj, TINYJS_TEMP_NAME);
			} else {
				l.match(CScriptLex.LEX_ID);
				if (l.tk == '(') {
					l.match('(');
					l.match(')');
				}
			}
		}
		// Nothing we can do here... just hope it's the end...
		l.match(CScriptLex.LEX_EOF);
		return null;
	}


    private CScriptVarLink unary(boolean execute){
		CScriptVarLink a;
		if (l.tk=='!') {
			l.match('!'); // binary not
			a = factor(execute);
			if (execute) {
				CScriptVar zero = new CScriptVar(0);
				CScriptVar res = a.var.mathsOp(zero, CScriptLex.LEX_EQUAL);
				a = createLink(a, res);
			}
		} else
			a = factor(execute);
		return a;
	}

    private CScriptVarLink term(boolean execute){
		CScriptVarLink a = unary(execute);
		while (l.tk=='*' || l.tk=='/' || l.tk=='%') {
			int op = l.tk;
			l.match(l.tk);
			CScriptVarLink b = unary(execute);
			if (execute) {
				CScriptVar res = a.var.mathsOp(b.var, op);
				a = createLink(a, res);
			}
		}
		return a;
	}

    private CScriptVarLink expression(boolean execute){
		boolean negate = false;
		if (l.tk=='-') {
			l.match('-');
			negate = true;
		}
		CScriptVarLink a = term(execute);
		if (negate) {
			CScriptVar zero = new CScriptVar(0);
			CScriptVar res = zero.mathsOp(a.var, '-');
			a = createLink(a, res);
		}

		while (l.tk=='+' || l.tk=='-' ||
			   l.tk==CScriptLex.LEX_PLUSPLUS || l.tk==CScriptLex.LEX_MINUSMINUS) {
			int op = l.tk;
			l.match(l.tk);
			if (op==CScriptLex.LEX_PLUSPLUS || op==CScriptLex.LEX_MINUSMINUS) {
				if (execute) {
					CScriptVar one = new CScriptVar(1);
					CScriptVar res = a.var.mathsOp(one, op==CScriptLex.LEX_PLUSPLUS ? '+' : '-');
					// in-place add/subtract
					a.replaceWith(res);
				}
			} else {
				CScriptVarLink b = term(execute);
				if (execute) {
					// not in-place, so just replace
					CScriptVar res = a.var.mathsOp(b.var, op);
					a = createLink(a, res);
				}
			}
		}
		return a;
	}

    private CScriptVarLink condition(boolean execute){
		CScriptVarLink a = expression(execute);
		CScriptVarLink b;
		while (l.tk==CScriptLex.LEX_EQUAL || l.tk==CScriptLex.LEX_NEQUAL ||
			   l.tk==CScriptLex.LEX_TYPEEQUAL || l.tk==CScriptLex.LEX_NTYPEEQUAL ||
			   l.tk==CScriptLex.LEX_LEQUAL || l.tk==CScriptLex.LEX_GEQUAL ||
			   l.tk=='<' || l.tk=='>') {
			int op = l.tk;
			l.match(l.tk);
			b = expression(execute);
			if (execute) {
				CScriptVar res = a.var.mathsOp(b.var, op);
				a = createLink(a,res);
			}
		}
		return a;
	}

    private CScriptVarLink logic(boolean execute){
		CScriptVarLink a = condition(execute);
		CScriptVarLink b;
		while (l.tk=='&' || l.tk=='|' || l.tk=='^' || l.tk==CScriptLex.LEX_ANDAND || l.tk==CScriptLex.LEX_OROR) {
			boolean noexecute = false;
			int op = l.tk;
			l.match(l.tk);
			boolean shortCircuit = false;
			boolean booolean = false;
			// if we have short-circuit ops, then if we know the outcome
			// we don't bother to execute the other op. Even if not
			// we need to tell mathsOp it's an & or |
			if (op==CScriptLex.LEX_ANDAND) {
				op = '&';
				shortCircuit = !a.var.getBool();
				booolean = true;
			} else if (op==CScriptLex.LEX_OROR) {
				op = '|';
				shortCircuit = a.var.getBool();
				booolean = true;
			}
			b = condition(shortCircuit ? noexecute : execute);
			if (execute && !shortCircuit) {
				if (booolean) {
					CScriptVar newa = new CScriptVar(a.var.getBool());
					CScriptVar newb = new CScriptVar(b.var.getBool());
					a = createLink(a, newa);
					b = createLink(b, newb);
				}
				CScriptVar res = a.var.mathsOp(b.var, op);
				a = createLink(a, res);
			}
		}
		return a;
	}

    private CScriptVarLink base(boolean execute){
		CScriptVarLink lhs = logic(execute);
		if (l.tk=='=' || l.tk==CScriptLex.LEX_PLUSEQUAL || l.tk==CScriptLex.LEX_MINUSEQUAL) {
			/* If we're assigning to this and we don't have a parent,
			 * add it to the symbol table root as per JavaScript. */
			if (execute && !lhs.owned) {
				if (lhs.name.length()>0) {
					CScriptVarLink realLhs = root.addChildNoDup(lhs.name, lhs.var);
					lhs = realLhs;
				} else
					System.err.println("Trying to assign to an un-named type\n");
			}

			int op = l.tk;
			l.match(l.tk);
			CScriptVarLink rhs = base(execute);
			if (execute) {
				if (op=='=') {
					lhs.replaceWith(rhs);
				} else if (op==CScriptLex.LEX_PLUSEQUAL) {
					CScriptVar res = lhs.var.mathsOp(rhs.var, '+');
					lhs.replaceWith(res);
				} else if (op==CScriptLex.LEX_MINUSEQUAL) {
					CScriptVar res = lhs.var.mathsOp(rhs.var, '-');
					lhs.replaceWith(res);
				} else assert false;
			}
		}
		return lhs;
	}

    private void block(boolean execute){
		l.match('{');
		if (execute) {
			while ((l.tk != 0) && l.tk!='}')
				statement(execute);
			l.match('}');
		} else {
			// fast skip of blocks
			int brackets = 1;
			while ((l.tk != 0) && (brackets !=0)) {
				if (l.tk == '{') brackets++;
				if (l.tk == '}') brackets--;
				l.match(l.tk);
			}
		}
	}

    private void statement(boolean execute){
		if (l.tk==CScriptLex.LEX_ID ||
			l.tk==CScriptLex.LEX_INT ||
			l.tk==CScriptLex.LEX_FLOAT ||
			l.tk==CScriptLex.LEX_STR ||
			l.tk=='-') {
			/* Execute a simple statement that only contains basic arithmetic... */
			l.match(';');
		} else if (l.tk=='{') {
			/* A block of code */
			block(execute);
		} else if (l.tk==';') {
			/* Empty statement - to allow things like ;;; */
			l.match(';');
		} else if (l.tk==CScriptLex.LEX_R_VAR) {
			/* variable creation. TODO - we need a better way of parsing the left
			 * hand side. Maybe just have a flag called can_create_var that we
			 * set and then we parse as if we're doing a normal equals.*/
			l.match(CScriptLex.LEX_R_VAR);
			CScriptVarLink a = null;
			if (execute)
				a = back(scopes).findChildOrCreate(l.tkStr, CScriptVar.SCRIPTVAR_UNDEFINED);
			l.match(CScriptLex.LEX_ID);
			// now do stuff defined with dots
			while (l.tk == '.') {
				l.match('.');
				if (execute) {
					CScriptVarLink lastA = a;
					a = lastA.var.findChildOrCreate(l.tkStr, CScriptVar.SCRIPTVAR_UNDEFINED);
				}
				l.match(CScriptLex.LEX_ID);
			}
			// sort out initialiser
			if (l.tk == '=') {
				l.match('=');
				CScriptVarLink var = base(execute);
				if (execute)
					a.replaceWith(var);
			}
			l.match(';');
		} else if (l.tk==CScriptLex.LEX_R_IF) {
			l.match(CScriptLex.LEX_R_IF);
			l.match('(');
			CScriptVarLink var = base(execute);
			l.match(')');
			boolean cond = execute && var.var.getBool();
			boolean noexecute = false; // because we need to be abl;e to write to it
			statement(cond ? execute : noexecute);
			if (l.tk==CScriptLex.LEX_R_ELSE) {
				l.match(CScriptLex.LEX_R_ELSE);
				statement(cond ? noexecute : execute);
			}
		} else if (l.tk==CScriptLex.LEX_R_WHILE) {
			// We do repetition by pulling out the string representing our statement
			// there's definitely some opportunity for optimisation here
			l.match(CScriptLex.LEX_R_WHILE);
			l.match('(');
			int whileCondStart = l.tokenStart;
			boolean noexecute = false;
			CScriptVarLink cond = base(execute);
			boolean loopCond = execute && cond.var.getBool();
			CScriptLex whileCond = l.getSubLex(whileCondStart);
			l.match(')');
			int whileBodyStart = l.tokenStart;
			statement(loopCond ? execute : noexecute);
			CScriptLex whileBody = l.getSubLex(whileBodyStart);
			CScriptLex oldLex = l;
			int loopCount = TINYJS_LOOP_MAX_ITERATIONS;
			while (loopCond && loopCount-- > 0) {
				whileCond.reset();
				l = whileCond;
				cond = base(execute);
				loopCond = execute && cond.var.getBool();
				if (loopCond) {
					whileBody.reset();
					l = whileBody;
					statement(execute);
				}
			}
			l = oldLex;
			whileCond.delete();
			whileBody.delete();

			if (loopCount<=0) {
				root.trace("", "");
				System.err.println("WHILE Loop exceeded " + 
								   TINYJS_LOOP_MAX_ITERATIONS + 
								   " iterations at " + l.getPosition(-1));
				throw new CScriptException("LOOP_ERROR");
			}
		} else if (l.tk==CScriptLex.LEX_R_FOR) {
			l.match(CScriptLex.LEX_R_FOR);
			l.match('(');
			statement(execute); // initialisation
			//l.match(';');
			int forCondStart = l.tokenStart;
			boolean noexecute = false;
			CScriptVarLink cond = base(execute); // condition
			boolean loopCond = execute && cond.var.getBool();
			CScriptLex forCond = l.getSubLex(forCondStart);
			l.match(';');
			int forIterStart = l.tokenStart;
			base(noexecute); // iterator
			CScriptLex forIter = l.getSubLex(forIterStart);
			l.match(')');
			int forBodyStart = l.tokenStart;
			statement(loopCond ? execute : noexecute);
			CScriptLex forBody = l.getSubLex(forBodyStart);
			CScriptLex oldLex = l;
			if (loopCond) {
				forIter.reset();
				l = forIter;
				base(execute);
			}
			int loopCount = TINYJS_LOOP_MAX_ITERATIONS;
			while (execute && loopCond && loopCount-- > 0) {
				forCond.reset();
				l = forCond;
				cond = base(execute);
				loopCond = cond.var.getBool();
				if (execute && loopCond) {
					forBody.reset();
					l = forBody;
					statement(execute);
				}
				if (execute && loopCond) {
					forIter.reset();
					l = forIter;
					base(execute);
				}
			}
			l = oldLex;
			forCond.delete();
			forIter.delete();
			forBody.delete();
			if (loopCount<=0) {
				root.trace("", "");
				System.err.println("FOR Loop exceeded " + TINYJS_LOOP_MAX_ITERATIONS + " iterations at " + l.getPosition(-1));
				throw new CScriptException("LOOP_ERROR");
			}
		} else if (l.tk==CScriptLex.LEX_R_RETURN) {
			l.match(CScriptLex.LEX_R_RETURN);
			CScriptVarLink result = null;
			if (l.tk != ';')
				result = base(execute);
			if (execute) {
				CScriptVarLink resultVar = back(scopes).findChild(TINYJS_RETURN_VAR);
				if (resultVar != null)
					resultVar.replaceWith(result);
				else
					System.err.println("RETURN statement, but not in a function.\n");
				execute = false;
			}
			l.match(';');
		} else if (l.tk==CScriptLex.LEX_R_FUNCTION) {
			CScriptVarLink funcVar = parseFunctionDefinition();
			if (execute) {
				if (funcVar.name == TINYJS_TEMP_NAME)
					System.err.println("Functions defined at statement-level are meant to have a name\n");
				else
					back(scopes).addChildNoDup(funcVar.name, funcVar.var);
			}
		} else l.match(CScriptLex.LEX_EOF);
	}

    // parsing utility functions
    private CScriptVarLink parseFunctionDefinition(){
		// actually parse a function...
		l.match(CScriptLex.LEX_R_FUNCTION);
		String funcName = TINYJS_TEMP_NAME;
		/* we can have functions without names */
		if (l.tk==CScriptLex.LEX_ID) {
			funcName = l.tkStr;
			l.match(CScriptLex.LEX_ID);
		}
		CScriptVarLink funcVar = new CScriptVarLink(
			new CScriptVar(TINYJS_BLANK_DATA, 
						   CScriptVar.SCRIPTVAR_FUNCTION), funcName);
		parseFunctionArguments(funcVar.var);
		int funcBegin = l.tokenStart;
		boolean noexecute = false;
		block(noexecute);
		funcVar.var.data = l.getSubString(funcBegin);
		return funcVar;
	}

    private void parseFunctionArguments(CScriptVar funcVar){
		l.match('(');
		while (l.tk!=')') {
			funcVar.addChildNoDup(l.tkStr, null);
			l.match(CScriptLex.LEX_ID);
			if (l.tk!=')') l.match(',');
		}
		l.match(')');		
	}

	///< Finds a child, looking recursively up the scopes
    private CScriptVarLink findInScopes(String childName){
		for (int s = scopes.size()-1; s >= 0;s--) {
			CScriptVarLink v = scopes.get(s).findChild(childName);
			if (v != null) return v;
		}
		return null;
	}

    /// Look up in any parent classes of the given object
    private CScriptVarLink findInParentClasses(CScriptVar object, String name){

		// Look for links to actual parent classes
		CScriptVarLink parentClass = object.findChild(TINYJS_PROTOTYPE_CLASS);
		while (parentClass != null) {
			CScriptVarLink implementation = parentClass.var.findChild(name);
			if (implementation != null) return implementation;
			parentClass = parentClass.var.findChild(TINYJS_PROTOTYPE_CLASS);
		}

		// else fake it for strings and finally objects
		if (object.isString()) {
			CScriptVarLink implementation = stringClass.findChild(name);
			if (implementation != null) return implementation;
		}

		if (object.isArray()) {
			CScriptVarLink implementation = arrayClass.findChild(name);
			if (implementation != null) return implementation;
		}

		CScriptVarLink implementation = objectClass.findChild(name);
		if (implementation != null) return implementation;

		return null;
	}

}
