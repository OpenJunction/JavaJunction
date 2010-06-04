package edu.stanford.junction.sample.extra;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class Encryption extends JunctionExtra {
	/**
	 * If an invitation is accepted, auto-detect whether
	 * to use encryption via a parameter "aes=[key]" in the invitation.
	 */
	public final static String FIELD_ENC = "e";
	public final static String FIELD_IV = "iv";
	public final static String URL_KEY_PARAM = "skey";

	private Cipher mCipher = null;
	private SecretKeySpec mKeySpec = null;
	protected byte[] mKey = null;

	public Encryption() {

	}

	private Encryption(byte[] key) {
		mKey=key;
		init();
	}

	private void init() {
		try {
			mCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			mKeySpec = new SecretKeySpec(mKey, "AES");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			SwitchboardConfig config = new XMPPSwitchboardConfig("prpl.stanford.edu");
			JunctionMaker jm = JunctionMaker.getInstance(config);


			JunctionActor rec = new JunctionActor("Recevier") {
				@Override
				public void onMessageReceived(MessageHeader header,
						JSONObject message) {
					System.out.println("rec got " + message.toString());
				}

				@Override
				public List<JunctionExtra> getInitialExtras() {
					List<JunctionExtra> e = super.getInitialExtras();
					e.add(new Encryption());
					return e;
				}

				@Override
				public void onActivityCreate() {
					System.out.println("Receiver created");
				}
			};

			JunctionActor send = new JunctionActor("Sender") {
				@Override
				public void onMessageReceived(MessageHeader header,
						JSONObject message) {
					System.out.println("send got " + message.toString());
				}

				@Override
				public void onActivityJoin() {
					try {
						JSONObject message = new JSONObject("{\"msg\":\"hello!! encrypted!\"}");
						sendMessageToSession(message);
						message = new JSONObject("{\"msg\":\"hello!! encrypted!\"}");
						sendMessageToSession(message);
						message = new JSONObject("{\"msg\":\"hello!! encrypted!\"}");
						sendMessageToSession(message);

						message = new JSONObject("{\"msg\":\"Keep the cryptotimes rollin!\",\"more\":\"mannnn\"}");
						sendMessageToSession(message);
						message = new JSONObject("{\"msg\":\"Keep the cryptotimes rollin!\",\"more\":\"mannnn\"}");
						sendMessageToSession(message);
					} catch (Exception e) {}
				}

				@Override
				public List<JunctionExtra> getInitialExtras() {
					List<JunctionExtra> e = super.getInitialExtras();
					e.add(new Encryption());
					return e;
				}

				@Override
				public void onActivityCreate() {
					System.out.println("Sender created");
				}
			};

			ActivityScript myScript = new ActivityScript();
			myScript.setActivityID("edu.stanford.junction.cryptdemo");
			myScript.setFriendlyName("CryptDemo");
			myScript.setSessionID("cryptosess");

			URI mySession = new URI("junction://prpl.stanford.edu/cryptosess?skey=XPVisDpGE82GYc8nCcgj%2FQ%3D%3D");
			jm.newJunction(mySession, rec);
			//jm.newJunction(myScript, rec);
			URI invite = rec.getJunction().getInvitationURI();

			System.out.println("created invitation " + invite);
			jm.newJunction(invite,send);

			synchronized(send){
				send.wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	

	@Override
	public boolean beforeActivityJoin() {
		// TODO: probably better to have mCreated or something.
		if (mKey != null) return true;

		try {
			URI invite = getActor().getJunction().getAcceptedInvitation();
			System.out.println("JOINING " + invite);
			if (invite != null) {
				String params = invite.getQuery();
				QueryString qs = new QueryString(params);
				String b64key = qs.getParameter(URL_KEY_PARAM);
				if (b64key == null) return true;

				mKey = Base64Coder.decode(b64key);
				init();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public boolean beforeActivityCreate() {
		try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(128);
			SecretKey skey = kgen.generateKey();

			mKey = skey.getEncoded();
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Encrypts a message before sending it over the wire.
	 */
	@Override
	public synchronized boolean beforeSendMessage(JSONObject msg) {
		if (mKeySpec == null) return true;

		try {
			String msgStr = msg.toString();

			mCipher.init(Cipher.ENCRYPT_MODE, mKeySpec);


			byte[] enc = null;
			enc = mCipher.doFinal(msgStr.getBytes());
			String encStr = new String(Base64Coder.encode(enc));
			String ivStr = new String(Base64Coder.encode(mCipher.getIV()));
			// clear object
			JSONArray keys = msg.names();
			for (int i=0;i<keys.length();i++) {
				msg.remove(keys.getString(i));
			}

			msg.put(FIELD_ENC,encStr);
			msg.put(FIELD_IV,ivStr);
		} catch (Exception e) {
			e.printStackTrace();
		}


		return true;
	}

	/**
	 * Decrypts an inbound message before handing it to the activity developer.
	 */
	@Override
	public synchronized boolean beforeOnMessageReceived(MessageHeader h, JSONObject msg) {
		if (mKeySpec == null) return true;

		try {
			if (!msg.has(FIELD_ENC)) {
				return true;
			}

			String b64 = msg.getString(FIELD_ENC);
			byte[] dec = Base64Coder.decode(b64);

			if (msg.has(FIELD_IV)) {
				byte[] iv = Base64.decode(msg.getString(FIELD_IV));
				mCipher.init(Cipher.DECRYPT_MODE, mKeySpec,new IvParameterSpec(iv));
				msg.remove(FIELD_IV);
			} else {
				mCipher.init(Cipher.DECRYPT_MODE, mKeySpec);
			}

			byte[] res = mCipher.doFinal(dec);
			JSONObject obj = new JSONObject(new String(res));

			msg.remove("e");
			Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				msg.put(key, obj.get(key));
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Low priority so we don't interfere with other extras
	 */
	@Override
	public Integer getPriority() {
		return 3;
	}

	@Override
	public void updateInvitationParameters(Map<String, String> params) {
		if (mKey!=null){
			String b64 = new String(Base64Coder.encode(mKey));
			params.put(URL_KEY_PARAM,b64);
		}
	}
}



class QueryString {

	private Map<String, List<String>> parameters;

	public QueryString(String qs) {
		parameters = new TreeMap<String, List<String>>();

		// Parse query string
		String pairs[] = qs.split("&");
		for (String pair : pairs) {
			String name;
			String value;
			int pos = pair.indexOf('=');
			// for "n=", the value is "", for "n", the value is null
			if (pos == -1) {
				name = pair;
				value = null;
			} else {
				try {
					name = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
					value = URLDecoder.decode(pair.substring(pos+1, pair.length()), "UTF-8");            
				} catch (UnsupportedEncodingException e) {
					// Not really possible, throw unchecked
					throw new IllegalStateException("No UTF-8");
				}
			}
			List<String> list = parameters.get(name);
			if (list == null) {
				list = new ArrayList<String>();
				parameters.put(name, list);
			}
			list.add(value);
		}
	}

	public String getParameter(String name) {        
		List<String> values = parameters.get(name);
		if (values == null)
			return null;

		if (values.size() == 0)
			return "";

		return values.get(0);
	}

	public String[] getParameterValues(String name) {        
		List<String> values = parameters.get(name);
		if (values == null)
			return null;

		return (String[])values.toArray(new String[values.size()]);
	}

	public Enumeration<String> getParameterNames() {  
		return Collections.enumeration(parameters.keySet()); 
	}

	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> map = new TreeMap<String, String[]>();
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			List<String> list = entry.getValue();
			String[] values;
			if (list == null)
				values = null;
			else
				values = (String[]) list.toArray(new String[list.size()]);
			map.put(entry.getKey(), values);
		}
		return map;
	} 
}

//Copyright 2003-2009 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
//www.source-code.biz, www.inventec.ch/chdh
//
//This module is multi-licensed and may be used under the terms
//of any of the following licenses:
//
//EPL, Eclipse Public License, http://www.eclipse.org/legal
//LGPL, GNU Lesser General Public License, http://www.gnu.org/licenses/lgpl.html
//AL, Apache License, http://www.apache.org/licenses
//BSD, BSD License, http://www.opensource.org/licenses/bsd-license.php
//
//Please contact the author if you need another license.
//This module is provided "as is", without warranties of any kind.

/**
* A Base64 Encoder/Decoder.
*
* <p>
* This class is used to encode and decode data in Base64 format as described in RFC 1521.
*
* <p>
* Home page: <a href="http://www.source-code.biz">www.source-code.biz</a><br>
* Author: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland<br>
* Multi-licensed: EPL/LGPL/AL/BSD.
*
* <p>
* Version history:<br>
* 2003-07-22 Christian d'Heureuse (chdh): Module created.<br>
* 2005-08-11 chdh: Lincense changed from GPL to LGPL.<br>
* 2006-11-21 chdh:<br>
*  &nbsp; Method encode(String) renamed to encodeString(String).<br>
*  &nbsp; Method decode(String) renamed to decodeString(String).<br>
*  &nbsp; New method encode(byte[],int) added.<br>
*  &nbsp; New method decode(String) added.<br>
* 2009-07-16: Additional licenses (EPL/AL) added.<br>
* 2009-09-16: Additional license (BSD) added.<br>
*/

class Base64Coder {

//Mapping table from 6-bit nibbles to Base64 characters.
private static char[]    map1 = new char[64];
static {
   int i=0;
   for (char c='A'; c<='Z'; c++) map1[i++] = c;
   for (char c='a'; c<='z'; c++) map1[i++] = c;
   for (char c='0'; c<='9'; c++) map1[i++] = c;
   map1[i++] = '+'; map1[i++] = '/'; }

//Mapping table from Base64 characters to 6-bit nibbles.
private static byte[]    map2 = new byte[128];
static {
   for (int i=0; i<map2.length; i++) map2[i] = -1;
   for (int i=0; i<64; i++) map2[map1[i]] = (byte)i; }

/**
* Encodes a string into Base64 format.
* No blanks or line breaks are inserted.
* @param s  a String to be encoded.
* @return   A String with the Base64 encoded data.
*/
public static String encodeString (String s) {
return new String(encode(s.getBytes())); }

/**
* Encodes a byte array into Base64 format.
* No blanks or line breaks are inserted.
* @param in  an array containing the data bytes to be encoded.
* @return    A character array with the Base64 encoded data.
*/
public static char[] encode (byte[] in) {
return encode(in,in.length); }

/**
* Encodes a byte array into Base64 format.
* No blanks or line breaks are inserted.
* @param in   an array containing the data bytes to be encoded.
* @param iLen number of bytes to process in <code>in</code>.
* @return     A character array with the Base64 encoded data.
*/
public static char[] encode (byte[] in, int iLen) {
int oDataLen = (iLen*4+2)/3;       // output length without padding
int oLen = ((iLen+2)/3)*4;         // output length including padding
char[] out = new char[oLen];
int ip = 0;
int op = 0;
while (ip < iLen) {
   int i0 = in[ip++] & 0xff;
   int i1 = ip < iLen ? in[ip++] & 0xff : 0;
   int i2 = ip < iLen ? in[ip++] & 0xff : 0;
   int o0 = i0 >>> 2;
   int o1 = ((i0 &   3) << 4) | (i1 >>> 4);
   int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
   int o3 = i2 & 0x3F;
   out[op++] = map1[o0];
   out[op++] = map1[o1];
   out[op] = op < oDataLen ? map1[o2] : '='; op++;
   out[op] = op < oDataLen ? map1[o3] : '='; op++; }
return out; }

/**
* Decodes a string from Base64 format.
* @param s  a Base64 String to be decoded.
* @return   A String containing the decoded data.
* @throws   IllegalArgumentException if the input is not valid Base64 encoded data.
*/
public static String decodeString (String s) {
return new String(decode(s)); }

/**
* Decodes a byte array from Base64 format.
* @param s  a Base64 String to be decoded.
* @return   An array containing the decoded data bytes.
* @throws   IllegalArgumentException if the input is not valid Base64 encoded data.
*/
public static byte[] decode (String s) {
return decode(s.toCharArray()); }

/**
* Decodes a byte array from Base64 format.
* No blanks or line breaks are allowed within the Base64 encoded data.
* @param in  a character array containing the Base64 encoded data.
* @return    An array containing the decoded data bytes.
* @throws    IllegalArgumentException if the input is not valid Base64 encoded data.
*/
public static byte[] decode (char[] in) {
int iLen = in.length;
if (iLen%4 != 0) throw new IllegalArgumentException ("Length of Base64 encoded input string is not a multiple of 4.");
while (iLen > 0 && in[iLen-1] == '=') iLen--;
int oLen = (iLen*3) / 4;
byte[] out = new byte[oLen];
int ip = 0;
int op = 0;
while (ip < iLen) {
   int i0 = in[ip++];
   int i1 = in[ip++];
   int i2 = ip < iLen ? in[ip++] : 'A';
   int i3 = ip < iLen ? in[ip++] : 'A';
   if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
      throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
   int b0 = map2[i0];
   int b1 = map2[i1];
   int b2 = map2[i2];
   int b3 = map2[i3];
   if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
      throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
   int o0 = ( b0       <<2) | (b1>>>4);
   int o1 = ((b1 & 0xf)<<4) | (b2>>>2);
   int o2 = ((b2 &   3)<<6) |  b3;
   out[op++] = (byte)o0;
   if (op<oLen) out[op++] = (byte)o1;
   if (op<oLen) out[op++] = (byte)o2; }
return out; }

//Dummy constructor.
private Base64Coder() {}

} // end class Base64Coder