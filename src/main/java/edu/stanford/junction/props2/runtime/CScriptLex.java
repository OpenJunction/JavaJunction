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


public class CScriptLex {

    public CScriptLex(String input){
    }
    public CScriptLex(CScriptLex owner, int startChar, int endChar){
    }

    public static final int LEX_EOF = 0;
    public static final int LEX_ID = 256;
    public static final int LEX_INT = 1;
    public static final int LEX_FLOAT = 2;
    public static final int LEX_STR = 3;
    public static final int LEX_EQUAL = 4;
    public static final int LEX_TYPEEQUAL = 5;
    public static final int LEX_NEQUAL = 6;
    public static final int LEX_NTYPEEQUAL = 7;
    public static final int LEX_LEQUAL = 8;
    public static final int LEX_LSHIFT = 9;
    public static final int LEX_LSHIFTEQUAL = 10;
    public static final int LEX_GEQUAL = 11;
    public static final int LEX_RSHIFT = 12;
    public static final int LEX_RSHIFTEQUAL = 13;
    public static final int LEX_PLUSEQUAL = 14;
    public static final int LEX_MINUSEQUAL = 15;
    public static final int LEX_PLUSPLUS = 16;
    public static final int LEX_MINUSMINUS = 17;
    public static final int LEX_ANDEQUAL = 18;
    public static final int LEX_ANDAND = 19;
    public static final int LEX_OREQUAL = 20;
    public static final int LEX_OROR = 21;
    public static final int LEX_XOREQUAL = 22;
    public static final int LEX_R_LIST_START = 23; 
    public static final int LEX_R_IF = 23; // intentional duplicate
    public static final int LEX_R_ELSE = 24;
    public static final int LEX_R_DO = 25;
    public static final int LEX_R_WHILE = 26;
    public static final int LEX_R_FOR = 27;
    public static final int LEX_R_BREAK = 28;
    public static final int LEX_R_CONTINUE = 29;
    public static final int LEX_R_FUNCTION = 30;
    public static final int LEX_R_RETURN = 31;
    public static final int LEX_R_VAR = 32;
    public static final int LEX_R_TRUE = 33;
    public static final int LEX_R_FALSE = 34;
    public static final int LEX_R_NULL = 35;
    public static final int LEX_R_UNDEFINED = 36;
    public static final int LEX_R_NEW = 37;
    public static final int LEX_R_LIST_END = 38;

    public char currCh, nextCh;
    public int tk; ///< The type of the token that we have
    public int tokenStart; ///< Position in the data at the beginning of the token we have here
    public int tokenEnd; ///< Position in the data at the last character of the token we have here
    public int tokenLastEnd; ///< Position in the data at the last character of the last token
    public String tkStr; ///< Data contained in the token we have here

    public void match(int expected_tk); ///< Lexical match wotsit
    public static String getTokenStr(int token); ///< Get the string representation of the given token
    public void reset(); ///< Reset this lex so we can start again

    public String getSubString(int pos); ///< Return a sub-string from the given position up until right now
    public CScriptLex getSubLex(int lastPosition); ///< Return a sub-lexer from the given position up until right now

    protected String getPosition(int pos); ///< Return a string representing the position in lines and columns of the character pos given

    /* When we go into a loop, we use getSubLex to get a lexer for just the sub-part of the
       relevant string. This doesn't re-allocate and copy the string, but instead copies
       the data pointer and sets dataOwned to false, and dataStart/dataEnd to the relevant things. */
    protected String data; ///< Data string to get tokens from
    protected int dataStart, dataEnd; ///< Start and end position in data string
    protected bool dataOwned; ///< Do we own this data string?
    protected int dataPos; ///< Position in data (we CAN go past the end of the string here)

    protected void getNextCh();
    protected void getNextToken(); ///< Get the text token from our text string
}