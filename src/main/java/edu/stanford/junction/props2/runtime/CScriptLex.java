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

    /* When we go into a loop, we use getSubLex to get a lexer for just the sub-part of the
       relevant string. This doesn't re-allocate and copy the string, but instead copies
       the data pointer and sets dataOwned to false, and dataStart/dataEnd to the relevant things. */
    protected String data; ///< Data string to get tokens from
    protected int dataStart, dataEnd; ///< Start and end position in data string
    protected boolean dataOwned; ///< Do we own this data string?
    protected int dataPos; ///< Position in data (we CAN go past the end of the string here)


    public CScriptLex(String input){
		data = input;
		dataOwned = true;
		dataStart = 0;
		dataEnd = data.length();
		reset();
    }

    public CScriptLex(CScriptLex owner, int startChar, int endChar){
		data = owner.data;
		dataOwned = false;
		dataStart = startChar;
		dataEnd = endChar;
		reset();
    }

	public void delete(){}

	public static boolean isWhitespace(char ch) {
		return (ch==' ') || (ch=='\t') || (ch=='\n') || (ch=='\r');
	}

	public static boolean isNumeric(char ch) {
		return (ch>='0') && (ch<='9');
	}

	public static boolean isNumber(final String str) {
		for (int i=0;i<str.length();i++)
			if (!isNumeric(str.charAt(i))) return false;
		return true;
	}

	public static boolean isHexadecimal(char ch) {
		return ((ch>='0') && (ch<='9')) ||
			((ch>='a') && (ch<='f')) ||
			((ch>='A') && (ch<='F'));
	}

	public static boolean isAlpha(char ch) {
		return ((ch>='a') && (ch<='z')) || ((ch>='A') && (ch<='Z')) || ch=='_';
	}

	public static boolean isIDString(final String str) {
		if(str.length() == 0) return false;
		if(!isAlpha(str.charAt(0))) return false;
		for (int i = 0; i < str.length(); i++){
			if (!(isAlpha(str.charAt(i)) || isNumeric(str.charAt(i))))
				return false;
		}
		return true;
	}

	// public static replace(String str, char textFrom, const char *textTo) {
	// 	int sLen = strlen(textTo);
	// 	size_t p = str.find(textFrom);
	// 	while (p != string::npos) {
	// 		str = str.substr(0, p) + textTo + str.substr(p+1);
	// 		p = str.find(textFrom, p+sLen);
	// 	}
	// }

	/// convert the given string into a quoted string suitable for javascript
	public static String getJSString(final String str) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0 ;i < str.length(); i++) {
			String replaceWith = "";
			boolean replace = true;
			switch (str.charAt(i)) {
			case '\\': replaceWith = "\\\\"; break;
			case '\n': replaceWith = "\\n"; break;
			case '"': replaceWith = "\\\""; break;
			default: replace=false;
			}
			if (replace) {
				sb.append(replaceWith);
			}
			else{
				sb.append(str.charAt(i));
			}
		}
		return "\"" + sb.toString() + "\"";
	}


    /** Is the string alphanumeric */
	public static boolean isAlphaNum(final String str) {
		if (str.length()==0) return true;
		if (!isAlpha(str.charAt(0))) return false;
		for (int i = 0 ; i < str.length(); i++){
			if (!(isAlpha(str.charAt(i)) || isNumeric(str.charAt(i))))
				return false;
		}
		return true;
	}


	///< Lexical match wotsit
    public void match(int expected_tk) throws CScriptException{
		if (tk != expected_tk) {
			String errorString = "Got "  + getTokenStr(tk) + " expected " + 
				getTokenStr(expected_tk) + " at " + getPosition(tokenStart) + 
				" in '" + data + "'";
			throw new CScriptException(errorString);
		}
		getNextToken();
	}

	///< Get the string representation of the given token
    public static String getTokenStr(int token){
		if (token > 32 && token < 128) {
			String.valueOf((char)token);
		}
		switch (token) {
        case LEX_EOF : return "EOF";
        case LEX_ID : return "ID";
        case LEX_INT : return "INT";
        case LEX_FLOAT : return "FLOAT";
        case LEX_STR : return "STRING";
        case LEX_EQUAL : return "==";
        case LEX_TYPEEQUAL : return "===";
        case LEX_NEQUAL : return "!=";
        case LEX_NTYPEEQUAL : return "!==";
        case LEX_LEQUAL : return "<=";
        case LEX_LSHIFT : return "<<";
        case LEX_LSHIFTEQUAL : return "<<=";
        case LEX_GEQUAL : return ">=";
        case LEX_RSHIFT : return ">>";
        case LEX_RSHIFTEQUAL : return ">>=";
        case LEX_PLUSEQUAL : return "+=";
        case LEX_MINUSEQUAL : return "-=";
        case LEX_PLUSPLUS : return "++";
        case LEX_MINUSMINUS : return "--";
        case LEX_ANDEQUAL : return "&=";
        case LEX_ANDAND : return "&&";
        case LEX_OREQUAL : return "|=";
        case LEX_OROR : return "||";
        case LEX_XOREQUAL : return "^=";
			// reserved words
        case LEX_R_IF : return "if";
        case LEX_R_ELSE : return "else";
        case LEX_R_DO : return "do";
        case LEX_R_WHILE : return "while";
        case LEX_R_FOR : return "for";
        case LEX_R_BREAK : return "break";
        case LEX_R_CONTINUE : return "continue";
        case LEX_R_FUNCTION : return "function";
        case LEX_R_RETURN : return "return";
        case LEX_R_VAR : return "var";
        case LEX_R_TRUE : return "true";
        case LEX_R_FALSE : return "false";
        case LEX_R_NULL : return "null";
        case LEX_R_UNDEFINED : return "undefined";
        case LEX_R_NEW : return "new";
		}
		return "?[" + token + "]";
	}

	///< Reset this lex so we can start again
    public void reset(){
		dataPos = dataStart;
		tokenStart = 0;
		tokenEnd = 0;
		tokenLastEnd = 0;
		tk = 0;
		tkStr = "";
		getNextCh();
		getNextCh();
		getNextToken();
	}

	///< Return a sub-string from the given position up until right now
    public String getSubString(int lastPosition){
		int lastCharIdx = tokenLastEnd + 1;
		if (lastCharIdx < dataEnd) {
			return data.substring(lastPosition, lastCharIdx);
		} else {
			return data.substring(lastPosition);
		}
	}

    ///< Return a sub-lexer from the given position up until right now
    public CScriptLex getSubLex(int lastPosition){ 
		int lastCharIdx = tokenLastEnd + 1;
		if (lastCharIdx < dataEnd)
			return new CScriptLex( this, lastPosition, lastCharIdx);
		else
			return new CScriptLex( this, lastPosition, dataEnd );
	}

    ///< Return a string representing the position in lines and columns of the character pos given
    protected String getPosition(int pos){
		if (pos < 0) pos = tokenLastEnd;
		int line = 1, col = 1;
		for (int i = 0;i < pos; i++) {
			char ch;
			if (i < dataEnd)
				ch = data.charAt(i);
			else
				ch = 0;
			col++;
			if (ch == '\n') {
				line++;
				col = 0;
			}
		}
		return "(line: " + line + ", col: " + col + ")";
	}

    protected void getNextCh(){
		currCh = nextCh;
		if (dataPos < dataEnd)
			nextCh = data.charAt(dataPos);
		else
			nextCh = 0;
		dataPos++;
	}

    protected void getNextToken(){
		tk = LEX_EOF;
		tkStr = "";
		while ((currCh != 0) && isWhitespace(currCh)) getNextCh();
		// newline comments
		if (currCh=='/' && nextCh=='/') {
			while ((currCh != 0) && currCh!='\n') getNextCh();
			getNextCh();
			getNextToken();
			return;
		}
		// block comments
		if (currCh=='/' && nextCh=='*') {
			while ((currCh != 0) && (currCh!='*' || nextCh!='/')) getNextCh();
			getNextCh();
			getNextCh();
			getNextToken();
			return;
		}
		// record beginning of this token
		tokenStart = dataPos-2;
		// tokens
		if (isAlpha(currCh)) { //  IDs
			while (isAlpha(currCh) || isNumeric(currCh)) {
				tkStr += currCh;
				getNextCh();
			}
			tk = LEX_ID;
			if (tkStr.equals("if")) tk = LEX_R_IF;
			else if (tkStr.equals("else")) tk = LEX_R_ELSE;
			else if (tkStr.equals("do")) tk = LEX_R_DO;
			else if (tkStr.equals("while")) tk = LEX_R_WHILE;
			else if (tkStr.equals("for")) tk = LEX_R_FOR;
			else if (tkStr.equals("break")) tk = LEX_R_BREAK;
			else if (tkStr.equals("continue")) tk = LEX_R_CONTINUE;
			else if (tkStr.equals("function")) tk = LEX_R_FUNCTION;
			else if (tkStr.equals("return")) tk = LEX_R_RETURN;
			else if (tkStr.equals("var")) tk = LEX_R_VAR;
			else if (tkStr.equals("true")) tk = LEX_R_TRUE;
			else if (tkStr.equals("false")) tk = LEX_R_FALSE;
			else if (tkStr.equals("null")) tk = LEX_R_NULL;
			else if (tkStr.equals("undefined")) tk = LEX_R_UNDEFINED;
			else if (tkStr.equals("new")) tk = LEX_R_NEW;
		} else if (isNumeric(currCh)) { // Numbers
			boolean isHex = false;
			if (currCh=='0') { tkStr += currCh; getNextCh(); }
			if (currCh=='x') {
				isHex = true;
				tkStr += currCh; getNextCh();
			}
			tk = LEX_INT;
			while (isNumeric(currCh) || (isHex && isHexadecimal(currCh))) {
				tkStr += currCh;
				getNextCh();
			}
			if (!isHex && currCh=='.') {
				tk = LEX_FLOAT;
				tkStr += '.';
				getNextCh();
				while (isNumeric(currCh)) {
					tkStr += currCh;
					getNextCh();
				}
			}
			// do fancy e-style floating point
			if (!isHex && currCh=='e') {
				tk = LEX_FLOAT;
				tkStr += currCh; getNextCh();
				if (currCh=='-') { tkStr += currCh; getNextCh(); }
				while (isNumeric(currCh)) {
					tkStr += currCh; getNextCh();
				}
			}
		} else if (currCh=='"') {
			// strings...
			getNextCh();
			while ((currCh != 0) && currCh!='"') {
				if (currCh == '\\') {
					getNextCh();
					switch (currCh) {
					case 'n' : tkStr += '\n'; break;
					case '"' : tkStr += '"'; break;
					case '\\' : tkStr += '\\'; break;
					default: tkStr += currCh;
					}
				} else {
					tkStr += currCh;
				}
				getNextCh();
			}
			getNextCh();
			tk = LEX_STR;
		} else if (currCh=='\'') {
			// strings again...
			getNextCh();
			while ((currCh!=0) && currCh!='\'') {
				if (currCh == '\\') {
					getNextCh();
					switch (currCh) {
					case 'n' : tkStr += '\n'; break;
					case '\'' : tkStr += '\''; break;
					case '\\' : tkStr += '\\'; break;
					default: tkStr += currCh;
					}
				} else {
					tkStr += currCh;
				}
				getNextCh();
			}
			getNextCh();
			tk = LEX_STR;
		} else {
			// single chars
			tk = currCh;
			if (currCh != 0) getNextCh();
			if (tk=='=' && currCh=='=') { // ==
				tk = LEX_EQUAL;
				getNextCh();
				if (currCh=='=') { // ===
					tk = LEX_TYPEEQUAL;
					getNextCh();
				}
			} else if (tk=='!' && currCh=='=') { // !=
				tk = LEX_NEQUAL;
				getNextCh();
				if (currCh=='=') { // !==
					tk = LEX_NTYPEEQUAL;
					getNextCh();
				}
			} else if (tk=='<' && currCh=='=') {
				tk = LEX_LEQUAL;
				getNextCh();
			} else if (tk=='<' && currCh=='<') {
				tk = LEX_LSHIFT;
				getNextCh();
				if (currCh=='=') { // <<=
					tk = LEX_LSHIFTEQUAL;
					getNextCh();
				}
			} else if (tk=='>' && currCh=='=') {
				tk = LEX_GEQUAL;
				getNextCh();
			} else if (tk=='>' && currCh=='>') {
				tk = LEX_RSHIFT;
				getNextCh();
				if (currCh=='=') { // <<=
					tk = LEX_RSHIFTEQUAL;
					getNextCh();
				}
			}  else if (tk=='+' && currCh=='=') {
				tk = LEX_PLUSEQUAL;
				getNextCh();
			}  else if (tk=='-' && currCh=='=') {
				tk = LEX_MINUSEQUAL;
				getNextCh();
			}  else if (tk=='+' && currCh=='+') {
				tk = LEX_PLUSPLUS;
				getNextCh();
			}  else if (tk=='-' && currCh=='-') {
				tk = LEX_MINUSMINUS;
				getNextCh();
			} else if (tk=='&' && currCh=='=') {
				tk = LEX_ANDEQUAL;
				getNextCh();
			} else if (tk=='&' && currCh=='&') {
				tk = LEX_ANDAND;
				getNextCh();
			} else if (tk=='|' && currCh=='=') {
				tk = LEX_OREQUAL;
				getNextCh();
			} else if (tk=='|' && currCh=='|') {
				tk = LEX_OROR;
				getNextCh();
			} else if (tk=='^' && currCh=='=') {
				tk = LEX_XOREQUAL;
				getNextCh();
			}
		}
		/* This isn't quite right yet */
		tokenLastEnd = tokenEnd;
		tokenEnd = dataPos-3;
	}
}