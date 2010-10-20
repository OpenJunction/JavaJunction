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


class CTinyJS {

    public static final int TINYJS_LOOP_MAX_ITERATIONS = 8192;

    /// convert the given string into a quoted string suitable for javascript
    public static String getJSString(String str);

    public void execute(String code){}

    /** Evaluate the given code and return a link to a javascript object,
     * useful for (dangerous) JSON parsing. If nothing to return, will return
     * 'undefined' variable type. CScriptVarLink is returned as this will
     * automatically unref the result as it goes out of scope. If you want to
     * keep it, you must use ref() and unref() */
    CScriptVarLink evaluateComplex(String code){}

    /** Evaluate the given code and return a string. If nothing to return, will return
     * 'undefined' */
    public String evaluate(String code){}


    /// add a native function to be called from TinyJS
    /** example:
       \code
           void scRandInt(CScriptVar *c, void *userdata) { ... }
           tinyJS->addNative("function randInt(min, max)", scRandInt, 0);
       \endcode

       or

       \code
           void scSubstring(CScriptVar *c, void *userdata) { ... }
           tinyJS->addNative("function String.substring(lo, hi)", scSubstring, 0);
       \endcode
    */
    public void addNative(String funcDesc, JSCallback ptr, Object userdata);

    /// Get the value of the given variable, or return 0
    public String getVariable(String path);

    /// Send all variables to stdout
    public void trace();

    public CScriptVar root;   /// root of symbol table


    private CScriptLex l;             /// current lexer
    private ArrayList<CScriptVar> scopes; /// stack of scopes when parsing
    private CScriptVar stringClass; /// Built in string class
    private CScriptVar objectClass; /// Built in object class
    private CScriptVar arrayClass; /// Built in array class

    // parsing - in order of precedence
    private CScriptVarLink factor(bool execute);
    private CScriptVarLink unary(bool execute);
    private CScriptVarLink term(bool execute);
    private CScriptVarLink expression(bool execute);
    private CScriptVarLink condition(bool execute);
    private CScriptVarLink logic(bool execute);
    private CScriptVarLink base(bool execute);
    private void block(bool execute);
    private void statement(bool execute);
    // parsing utility functions
    private CScriptVarLink parseFunctionDefinition();
    private void parseFunctionArguments(CScriptVar funcVar);

    private CScriptVarLink findInScopes(String childName); ///< Finds a child, looking recursively up the scopes
    /// Look up in any parent classes of the given object
    private CScriptVarLink findInParentClasses(CScriptVar object, String name);

}
