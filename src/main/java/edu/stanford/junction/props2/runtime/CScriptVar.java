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
    public static final String TINYJS_RETURN_VAR = "return";
    public static final String TINYJS_PROTOTYPE_CLASS = "prototype";
    public static final String TINYJS_TEMP_NAME = "";
    public static final String TINYJS_BLANK_DATA = "";

    ///< Create undefined
    public CScriptVar(); 

    ///< User defined
    public CScriptVar(String varData, int varFlags); 

    ///< Create a string
    public CScriptVar(String str); 

    public CScriptVar(double varData);

    public CScriptVar(int val);

    ///< If this is a function, get the result value (for use by native functions)
    public CScriptVar getReturnVar(); 

    ///< Set the result value. Use this when setting complex return data as it avoids a deepCopy()
    public void setReturnVar(CScriptVar var); 

    ///< If this is a function, get the parameter with the given name (for use by native functions)
    public CScriptVar getParameter(String name); 
    
    ///< Tries to find a child with the given name, may return 0
    public CScriptVarLink findChild(String childName); 
    
    ///< Tries to find a child with the given name, or will create it with the given flags
    public CScriptVarLink findChildOrCreate(String childName, int varFlags); 
    
    ///< Tries to find a child with the given path (separated by dots)
    public CScriptVarLink findChildOrCreateByPath(String path); 
    public CScriptVarLink addChild(String childName, CScriptVar child);
    
    ///< add a child overwriting any with the same name
    public CScriptVarLink addChildNoDup(String childName, CScriptVar child); 
    public void removeChild(CScriptVar child);

    ///< Remove a specific link (this is faster than finding via a child)
    public void removeLink(CScriptVarLink link); 
    public void removeAllChildren();

    ///< The the value at an array index
    public CScriptVar getArrayIndex(int idx); 

    ///< Set the value at an array index
    public void setArrayIndex(int idx, CScriptVar value); 

    ///< If this is an array, return the number of items in it (else 0)
    public int getArrayLength(); 

    ///< Get the number of children
    public int getChildren(); 

    public int getInt();
    public bool getBool() { return getInt() != 0; }
    public double getDouble();
    public String getString();

    ///< get Data as a parsable javascript string
    public String getParsableString(); 

    public void setInt(int num);
    public void setDouble(double val);
    public void setString(String str);
    public void setUndefined();

    public bool isInt() { return (flags&SCRIPTVAR_INTEGER)!=0; }
    public bool isDouble() { return (flags&SCRIPTVAR_DOUBLE)!=0; }
    public bool isString() { return (flags&SCRIPTVAR_STRING)!=0; }
    public bool isNumeric() { return (flags&SCRIPTVAR_NUMERICMASK)!=0; }
    public bool isFunction() { return (flags&SCRIPTVAR_FUNCTION)!=0; }
    public bool isObject() { return (flags&SCRIPTVAR_OBJECT)!=0; }
    public bool isArray() { return (flags&SCRIPTVAR_ARRAY)!=0; }
    public bool isNative() { return (flags&SCRIPTVAR_NATIVE)!=0; }
    public bool isUndefined() { return (flags & SCRIPTVAR_VARTYPEMASK) == SCRIPTVAR_UNDEFINED; }
    public bool isNull() { return (flags & SCRIPTVAR_NULL)!=0; }

    ///< Is this *not* an array/object/etc
    public bool isBasic() { return firstChild==0; } 

    ///< do a maths op with another script variable
    public CScriptVar mathsOp(CScriptVar b, int op); 

    ///< copy the value from the value given
    public void copyValue(CScriptVar *val); 
    
    ///< deep copy this node and return the result
    public CScriptVar deepCopy(); 

    ///< Dump out the contents of this using trace
    public void trace(std::string indentStr = "", const std::string &name = ""); 

    ///< For debugging - just dump a string version of the flags
    public String getFlagsAsString(); 

    ///< Write out all the JS code needed to recreate this script variable to the stream (as JSON)
    public void getJSON(StringBuffer destination, String linePrefix); 

    ///< Set the callback for native functions
    public void setCallback(JSCallback callback, void userdata); 

    public CScriptVarLink firstChild;
    public CScriptVarLink lastChild;

    /// For memory management/garbage collection
    ///< Add reference to this variable
    public CScriptVar ref(); 
    ///< Remove a reference, and delete this variable if required
    public void unref(); 
    ///< Get the number of references to this script variable
    public int getRefs(); 

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

    ///< initialisation of data members
    protected void init(); 

    /** Copy the basic data and flags from the variable given, with no
     * children. Should be used internally only - by copyValue and deepCopy */
    protected void copySimpleData(CScriptVar *val);

}
