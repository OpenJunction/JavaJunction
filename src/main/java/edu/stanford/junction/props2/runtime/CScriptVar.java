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

/// Variable class (containing a doubly-linked list of children)
public class CScriptVar {

    public static final int SCRIPTVAR_UNDEFINED   = 0;
    public static final int SCRIPTVAR_FUNCTION    = 1;
    public static final int SCRIPTVAR_OBJECT      = 2;
    public static final int SCRIPTVAR_ARRAY       = 4;
    public static final int SCRIPTVAR_DOUBLE      = 8;  // floating point double
    public static final int SCRIPTVAR_INTEGER     = 16; // integer number
    public static final int SCRIPTVAR_STRING      = 32; // string
    public static final int SCRIPTVAR_NULL        = 64; // it seems null is its own data type
    public static final int SCRIPTVAR_NATIVE      = 128; // to specify this is a native function
    public static final int SCRIPTVAR_NUMERICMASK = SCRIPTVAR_NULL |
		SCRIPTVAR_DOUBLE |
		SCRIPTVAR_INTEGER;
    public static final int SCRIPTVAR_VARTYPEMASK = SCRIPTVAR_DOUBLE |
		SCRIPTVAR_INTEGER |
		SCRIPTVAR_STRING |
		SCRIPTVAR_FUNCTION |
		SCRIPTVAR_OBJECT |
		SCRIPTVAR_ARRAY |
		SCRIPTVAR_NULL;

    ///< Create undefined
    public CScriptVar(){
		init();
		flags = SCRIPTVAR_UNDEFINED;
		refs = 0;
	}

    ///< User defined
    public CScriptVar(String varData, int varFlags){
		refs = 0;
		init();
		flags = varFlags;
		if ((varFlags & SCRIPTVAR_INTEGER) != 0) {
			intData = Long.valueOf(varData);
		} else if ((varFlags & SCRIPTVAR_DOUBLE) != 0) {
			doubleData = Double.valueOf(varData);
		} else
			data = varData;
	}

    ///< Create a string
    public CScriptVar(String str){
		refs = 0;
		init();
		flags = SCRIPTVAR_STRING;
		data = str;
	}

    public CScriptVar(double varData){
		refs = 0;
		init();
		setDouble(varData);
	}

    public CScriptVar(int val){
		refs = 0;
		init();
		setInt(val);
	}

    public CScriptVar(boolean val){
		refs = 0;
		init();
		setInt(val?1:0);
	}

    ///< initialisation of data members
    protected void init(){
		firstChild = null;
		lastChild = null;
		flags = 0;
		jsCallback = null;
		jsCallbackUserData = null;
		data = CTinyJS.TINYJS_BLANK_DATA;
		intData = 0;
		doubleData = 0;
	}

    ///< If this is a function, get the result value (for use by native functions)
    public CScriptVar getReturnVar(){
		return getParameter(CTinyJS.TINYJS_RETURN_VAR);
	}

    ///< Set the result value. Use this when setting complex return data as it avoids a deepCopy()
    public void setReturnVar(CScriptVar var){
		findChildOrCreate(CTinyJS.TINYJS_RETURN_VAR, SCRIPTVAR_UNDEFINED).replaceWith(var);
	} 

    ///< If this is a function, get the parameter with the given name (for use by native functions)
    public CScriptVar getParameter(String name){
		return findChildOrCreate(name, SCRIPTVAR_UNDEFINED).var;
	} 
    
    ///< Tries to find a child with the given name, may return 0
    public CScriptVarLink findChild(String childName){
		CScriptVarLink v = firstChild;
		while (v != null) {
			if (v.name.equals(childName))
				return v;
			v = v.nextSibling;
		}
		return null;
	} 
    
    ///< Tries to find a child with the given name, or will create it with the given flags
    public CScriptVarLink findChildOrCreate(String childName, int varFlags){
		CScriptVarLink l = findChild(childName);
		if (l != null) return l;
		return addChild(childName, new CScriptVar(CTinyJS.TINYJS_BLANK_DATA, varFlags));
	} 
    
    ///< Tries to find a child with the given path (separated by dots)
    public CScriptVarLink findChildOrCreateByPath(String path){
		int p = path.indexOf('.');
		if (p == -1)
			return findChildOrCreate(path, SCRIPTVAR_UNDEFINED);

		return findChildOrCreate(path.substring(0, p), SCRIPTVAR_OBJECT).var.
			findChildOrCreateByPath(path.substring(p + 1, path.length() - 1));
	} 

    public CScriptVarLink addChild(String childName, CScriptVar child){
		if (isUndefined()) {
			flags = SCRIPTVAR_OBJECT;
		}
		// if no child supplied, create one
		if (child == null)
			child = new CScriptVar();

		CScriptVarLink link = new CScriptVarLink(child, childName);
		link.owned = true;
		if (lastChild != null) {
			lastChild.nextSibling = link;
			link.prevSibling = lastChild;
			lastChild = link;
		} else {
			firstChild = link;
			lastChild = link;
		}
		return link;
	}
    
    ///< add a child overwriting any with the same name
    public CScriptVarLink addChildNoDup(String childName, CScriptVar child){
		// if no child supplied, create one
		if (child == null)
			child = new CScriptVar();

		CScriptVarLink v = findChild(childName);
		if (v != null) {
			v.replaceWith(child);
		} else {
			v = addChild(childName, child);
		}

		return v;
	} 

    public void removeChild(CScriptVar child){
		CScriptVarLink link = firstChild;
		while (link != null) {
			if(link.var == child)
				break;
			link = link.nextSibling;
		}
		removeLink(link);
	}

    ///< Remove a specific link (this is faster than finding via a child)
    public void removeLink(CScriptVarLink link){
		if (link == null) return;
		if (link.nextSibling != null)
			link.nextSibling.prevSibling = link.prevSibling;
		if (link.prevSibling != null)
			link.prevSibling.nextSibling = link.nextSibling;
		if (lastChild == link)
			lastChild = link.prevSibling;
		if (firstChild == link)
			firstChild = link.nextSibling;
		link.delete();
	} 

    public void removeAllChildren(){
		CScriptVarLink c = firstChild;
		while (c != null) {
			CScriptVarLink t = c.nextSibling;
			c.delete();
			c = t;
		}
		firstChild = null;
		lastChild = null;
	}

    ///< The the value at an array index
    public CScriptVar getArrayIndex(int idx){
		CScriptVarLink link = findChild(String.valueOf(idx));
		if (link != null) return link.var;
		else return new CScriptVar(CTinyJS.TINYJS_BLANK_DATA, SCRIPTVAR_NULL); // undefined
	} 

    ///< Set the value at an array index
    public void setArrayIndex(int idx, CScriptVar value){
		String sIdx = String.valueOf(idx);
		CScriptVarLink link = findChild(sIdx);
		if (link != null) {
			if (value.isUndefined())
				removeLink(link);
			else
				link.replaceWith(value);
		} else {
			if (!value.isUndefined())
				addChild(sIdx, value);
		}
	} 

    ///< If this is an array, return the number of items in it (else 0)
    public int getArrayLength(){
		int highest = -1;
		if (!isArray()) return 0;
		CScriptVarLink link = firstChild;
		while (link != null) {
			if (CScriptLex.isNumber(link.name)) {
				int val = Integer.valueOf(link.name);
				if (val > highest) highest = val;
			}
			link = link.nextSibling;
		}
		return highest+1;
	} 

    ///< Get the number of children
    public int getChildren(){
		int n = 0;
		CScriptVarLink link = firstChild;
		while (link != null) {
			n++;
			link = link.nextSibling;
		}
		return n;
	} 

    public int getInt(){
		/* strtol understands about hex and octal */
		if (isInt()) return (int)intData;
		if (isNull()) return 0;
		if (isUndefined()) return 0;
		if (isDouble()) return (int)doubleData;
		return 0;
	}

    public boolean getBool() { return getInt() != 0; }

    public double getDouble(){
		if (isDouble()) return doubleData;
		if (isInt()) return intData;
		if (isNull()) return 0;
		if (isUndefined()) return 0;
		return 0; /* or NaN? */
	}

    public String getString(){
		/* Because we can't return a string that is generated on demand.
		 * I should really just use char* :) */
		String s_null = "null";
		String s_undefined = "undefined";
		if (isInt()) {
			return String.valueOf(intData);
		}
		if (isDouble()) {
			return String.valueOf(doubleData);
		}
		if (isNull()) return s_null;
		if (isUndefined()) return s_undefined;

		// are we just a string here?
		return data;
	}

    ///< get Data as a parsable javascript string
    public String getParsableString(){
		// Numbers can just be put in directly
		if (isNumeric())
			return getString();
		if (isFunction()) {
			String funcStr = "function (";
			// get list of parameters
			CScriptVarLink link = firstChild;
			while (link != null) {
				funcStr += link.name;
				if (link.nextSibling != null) funcStr += ",";
				link = link.nextSibling;
			}
			// add function body
			funcStr += ") " + getString();
			return funcStr;
		}
		// if it is a string then we quote it
		if (isString())
			return CScriptLex.getJSString(getString());
		if (isNull())
			return "null";
		return "undefined";
	} 

    public void setInt(int num){
		flags = (flags&~SCRIPTVAR_VARTYPEMASK) | SCRIPTVAR_INTEGER;
		intData = num;
		doubleData = 0;
		data = CTinyJS.TINYJS_BLANK_DATA;
	}

    public void setDouble(double val){
		flags = (flags&~SCRIPTVAR_VARTYPEMASK) | SCRIPTVAR_DOUBLE;
		doubleData = val;
		intData = 0;
		data = CTinyJS.TINYJS_BLANK_DATA;
	}

    public void setString(String str){
		// name sure it's not still a number or integer
		flags = (flags&~SCRIPTVAR_VARTYPEMASK) | SCRIPTVAR_STRING;
		data = str;
		intData = 0;
		doubleData = 0;
	}

    public void setUndefined(){
		// name sure it's not still a number or integer
		flags = (flags&~SCRIPTVAR_VARTYPEMASK) | SCRIPTVAR_UNDEFINED;
		data = CTinyJS.TINYJS_BLANK_DATA;
		intData = 0;
		doubleData = 0;
		removeAllChildren();
	}

    public boolean isInt() { return (flags&SCRIPTVAR_INTEGER)!=0; }
    public boolean isDouble() { return (flags&SCRIPTVAR_DOUBLE)!=0; }
    public boolean isString() { return (flags&SCRIPTVAR_STRING)!=0; }
    public boolean isNumeric() { return (flags&SCRIPTVAR_NUMERICMASK)!=0; }
    public boolean isFunction() { return (flags&SCRIPTVAR_FUNCTION)!=0; }
    public boolean isObject() { return (flags&SCRIPTVAR_OBJECT)!=0; }
    public boolean isArray() { return (flags&SCRIPTVAR_ARRAY)!=0; }
    public boolean isNative() { return (flags&SCRIPTVAR_NATIVE)!=0; }
    public boolean isUndefined() { return (flags & SCRIPTVAR_VARTYPEMASK) == SCRIPTVAR_UNDEFINED; }
    public boolean isNull() { return (flags & SCRIPTVAR_NULL)!=0; }

    ///< Is this *not* an array/object/etc
    public boolean isBasic() { return firstChild==null; } 

    ///< do a maths op with another script variable
    public CScriptVar mathsOp(CScriptVar b, int op) throws CScriptException{
		CScriptVar a = this;
		// Type equality check
		if (op == CScriptLex.LEX_TYPEEQUAL || op == CScriptLex.LEX_NTYPEEQUAL) {
			// check type first, then call again to check data
			boolean eql = ((a.flags & SCRIPTVAR_VARTYPEMASK) ==
						   (b.flags & SCRIPTVAR_VARTYPEMASK)) &&
				(a.mathsOp(b, CScriptLex.LEX_EQUAL) != null);
			if (op == CScriptLex.LEX_TYPEEQUAL)
				return new CScriptVar(eql);
			else
				return new CScriptVar(!eql);
		}
		// do maths...
		if (a.isUndefined() && b.isUndefined()) {
			if (op == CScriptLex.LEX_EQUAL) return new CScriptVar(true);
			else if (op == CScriptLex.LEX_NEQUAL) return new CScriptVar(false);
			else return new CScriptVar(); // undefined
		} else if ((a.isNumeric() || a.isUndefined()) &&
				   (b.isNumeric() || b.isUndefined())) {
			if (!a.isDouble() && !b.isDouble()) {
				// use ints
				int da = a.getInt();
				int db = b.getInt();
				switch (op) {
                case '+': return new CScriptVar(da+db);
                case '-': return new CScriptVar(da-db);
                case '*': return new CScriptVar(da*db);
                case '/': return new CScriptVar(da/db);
                case '&': return new CScriptVar(da&db);
                case '|': return new CScriptVar(da|db);
                case '^': return new CScriptVar(da^db);
                case '%': return new CScriptVar(da%db);
                case CScriptLex.LEX_EQUAL:     return new CScriptVar(da==db);
                case CScriptLex.LEX_NEQUAL:    return new CScriptVar(da!=db);
                case '<':     return new CScriptVar(da<db);
                case CScriptLex.LEX_LEQUAL:    return new CScriptVar(da<=db);
                case '>':     return new CScriptVar(da>db);
                case CScriptLex.LEX_GEQUAL:    return new CScriptVar(da>=db);
                default: throw new CScriptException("This operation not supported on the Int datatype");
				}
			} else {
				// use doubles
				double da = a.getDouble();
				double db = b.getDouble();
				switch (op) {
                case '+': return new CScriptVar(da+db);
                case '-': return new CScriptVar(da-db);
                case '*': return new CScriptVar(da*db);
                case '/': return new CScriptVar(da/db);
                case CScriptLex.LEX_EQUAL:     return new CScriptVar(da==db);
                case CScriptLex.LEX_NEQUAL:    return new CScriptVar(da!=db);
                case '<':     return new CScriptVar(da<db);
                case CScriptLex.LEX_LEQUAL:    return new CScriptVar(da<=db);
                case '>':     return new CScriptVar(da>db);
                case CScriptLex.LEX_GEQUAL:    return new CScriptVar(da>=db);
                default: throw new CScriptException("This operation not supported on the Double datatype");
				}
			}
		} else if (a.isArray()) {
			/* Just check pointers */
			switch (op) {
			case CScriptLex.LEX_EQUAL: return new CScriptVar(a==b);
			case CScriptLex.LEX_NEQUAL: return new CScriptVar(a!=b);
			default: throw new CScriptException("This operation not supported on the Array datatype");
			}
		} else if (a.isObject()) {
			/* Just check pointers */
			switch (op) {
			case CScriptLex.LEX_EQUAL: return new CScriptVar(a==b);
			case CScriptLex.LEX_NEQUAL: return new CScriptVar(a!=b);
			default: throw new CScriptException("This operation not supported on the Object datatype");
			}
		} else {
			String da = a.getString();
			String db = b.getString();
			int comp = da.compareTo(db);
			// use strings
			switch (op) {
			case '+':  return new CScriptVar(da+db, SCRIPTVAR_STRING);
			case CScriptLex.LEX_EQUAL:     return new CScriptVar(comp == 0);
			case CScriptLex.LEX_NEQUAL:    return new CScriptVar(comp != 0);
			case '<':     return new CScriptVar(comp < 0);
			case CScriptLex.LEX_LEQUAL:    return new CScriptVar(comp <= 0);
			case '>':     return new CScriptVar(comp > 0);
			case CScriptLex.LEX_GEQUAL:    return new CScriptVar(comp >= 0);
			default: throw new CScriptException("This operation not supported on the string datatype");
			}
		}
	} 

    ///< copy the value from the value given
    public void copyValue(CScriptVar val){
		if (val != null) {
			copySimpleData(val);
			// remove all current children
			removeAllChildren();
			// copy children of 'val'
			CScriptVarLink child = val.firstChild;
			while (child != null) {
				CScriptVar copied;
				// don't copy the 'parent' object...
				if (child.name != CTinyJS.TINYJS_PROTOTYPE_CLASS)
					copied = child.var.deepCopy();
				else
					copied = child.var;

				addChild(child.name, copied);
				child = child.nextSibling;
			}
		} else {
			setUndefined();
		}
	} 
    
    ///< deep copy this node and return the result
    public CScriptVar deepCopy(){
		CScriptVar newVar = new CScriptVar();
		newVar.copySimpleData(this);
		// copy children
		CScriptVarLink child = firstChild;
		while (child != null) {
			CScriptVar copied;
			// don't copy the 'parent' object...
			if (child.name != CTinyJS.TINYJS_PROTOTYPE_CLASS)
				copied = child.var.deepCopy();
			else
				copied = child.var;

			newVar.addChild(child.name, copied);
			child = child.nextSibling;
		}
		return newVar;
	} 

    ///< Dump out the contents of this using trace
    public void trace(String indentStr, String name){
		System.out.print(indentStr + name + " = '" + getString() + "' " + getFlagsAsString() + "\n");
		String indent = indentStr + " ";
		CScriptVarLink link = firstChild;
		while (link != null) {
			link.var.trace(indent, link.name);
			link = link.nextSibling;
		}
	} 

    ///< For debugging - just dump a string version of the flags
    public String getFlagsAsString(){
		String flagstr = "";
		if ((flags&SCRIPTVAR_FUNCTION) != 0) flagstr = flagstr + "FUNCTION ";
		if ((flags&SCRIPTVAR_OBJECT) != 0) flagstr = flagstr + "OBJECT ";
		if ((flags&SCRIPTVAR_ARRAY) != 0) flagstr = flagstr + "ARRAY ";
		if ((flags&SCRIPTVAR_NATIVE) != 0) flagstr = flagstr + "NATIVE ";
		if ((flags&SCRIPTVAR_DOUBLE) != 0) flagstr = flagstr + "DOUBLE ";
		if ((flags&SCRIPTVAR_INTEGER) != 0) flagstr = flagstr + "INTEGER ";
		if ((flags&SCRIPTVAR_STRING) != 0) flagstr = flagstr + "STRING ";
		return flagstr;
	} 

    ///< Write out all the JS code needed to recreate this script variable to the stream (as JSON)
    public void getJSON(StringBuffer destination, String linePrefix){
		if (isObject()) {
			String indentedLinePrefix = linePrefix + "  ";
			// children - handle with bracketed list
			destination.append("{ \n");
			CScriptVarLink link = firstChild;
			while (link != null) {
				destination.append(indentedLinePrefix);
				if (CScriptLex.isAlphaNum(link.name))
					destination.append(link.name);
				else
					destination.append(CScriptLex.getJSString(link.name));
				destination.append(" : ");
				link.var.getJSON(destination, indentedLinePrefix);
				link = link.nextSibling;
				if (link != null) {
					destination.append(",\n");
				}
			}
			destination.append("\n").append(linePrefix).append("}");
		} else if (isArray()) {
			String indentedLinePrefix = linePrefix + "  ";
			destination.append("[\n");
			int len = getArrayLength();
			if (len>10000) len=10000; // we don't want to get stuck here!

			for (int i = 0; i < len; i++) {
				getArrayIndex(i).getJSON(destination, indentedLinePrefix);
				if (i < len-1) destination.append(",\n");
			}

			destination.append("\n").append(linePrefix).append("]");
		} else {
			// no children or a function... just write value directly
			destination.append(getParsableString());
		}
	} 

    ///< Set the callback for native functions
    public void setCallback(JSCallback callback, Object userdata){
		jsCallback = callback;
		jsCallbackUserData = userdata;
	} 

    public JSCallback getJSCallback(){
		return jsCallback;
	} 

    public CScriptVarLink firstChild;
    public CScriptVarLink lastChild;

    /// For memory management/garbage collection
    ///< Add reference to this variable
    public CScriptVar ref(){
		refs++;
		return this;
	} 
    ///< Remove a reference, and delete this variable if required
    public void unref(){
		if (refs<=0) System.out.println("OMFG, we have unreffed too far!\n");
		if ((--refs)==0) {
			delete();
		}
	} 

	public void delete(){
		removeAllChildren();
	}

    ///< Get the number of references to this script variable
    public int getRefs(){
		return refs;
	} 

    protected int refs; ///< The number of references held to this - used for garbage collection

    ///< The contents of this variable if it is a string
    protected String data; 

    ///< The contents of this variable if it is an int
    protected long intData; 

    ///< The contents of this variable if it is a double
    protected double doubleData; 

    ///< the flags determine the type of the variable - int/double/string/etc
    protected int flags; 

    ///< Callback for native functions
    protected JSCallback jsCallback; 

    ///< user data passed as second argument to native functions
    protected Object jsCallbackUserData; 

    /** Copy the basic data and flags from the variable given, with no
     * children. Should be used internally only - by copyValue and deepCopy */
    protected void copySimpleData(CScriptVar val){
		data = val.data;
		intData = val.intData;
		doubleData = val.doubleData;
		flags = (flags & ~SCRIPTVAR_VARTYPEMASK) | (val.flags & SCRIPTVAR_VARTYPEMASK);
	}

}
