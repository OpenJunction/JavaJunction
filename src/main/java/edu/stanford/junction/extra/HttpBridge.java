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


package edu.stanford.junction.extra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
/*
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
*/

// temporary to avoid errors, without including HttpServlet as dependency
class ServletInputStream extends InputStream {

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
}

class ServletOutputStream extends OutputStream {

	@Override
	public void write(int b) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public void print(String s) {}
	
}

class HttpServletRequest {
	public String getRequestURI() { return null; }
	public String getContentType() { return null; }
	public ServletInputStream getInputStream () { return null; }
}

class HttpServletResponse {
	public void setContentType(String type) {}
	
	
	public ServletOutputStream getOutputStream () { return null; }
}

class HttpServlet {
	protected void doPost(HttpServletRequest req,
			HttpServletResponse resp) {}
	
	protected void doGet(HttpServletRequest req,
			HttpServletResponse resp) {}
	
}

public class HttpBridge extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	// static makes the same hashmap available
	// to all connecting threads.
	
	// remember that a thread is created every time
	// a client connects to our socket.
	static HashMap<String,HttpBridgePair> bridge = 
		new HashMap<String,HttpBridgePair>();

	
	@Override
	protected void doPost(HttpServletRequest req,
			HttpServletResponse resp) {
	
		String uri = req.getRequestURI();
		HttpBridgePair pair;
		if (!bridge.containsKey(uri)) {
			pair = new HttpBridgePair();
			bridge.put(uri, pair);
		} else {
			pair = bridge.get(uri);
		}
		
		if (pair.hasPoster()) {
			// throw error
			return;
		}
		
		pair.setPoster(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req,
			HttpServletResponse resp) {
		
		String uri = req.getRequestURI();
		HttpBridgePair pair;
		if (!bridge.containsKey(uri)) {
			pair = new HttpBridgePair();
			bridge.put(uri, pair);
		} else {
			pair = bridge.get(uri);
		}
		
		if (pair.hasGetter()) {
			// throw error
			return;
		}
		
		pair.setGetter(req, resp);
		bridge.remove(uri);
	}
}

class HttpBridgePair {
	final private int BUFFER_SIZE = 2048; // bytes
	private HTTPClientConnection mPoster;
	private HTTPClientConnection mGetter;
	
	public boolean hasGetter() {
		return (mGetter != null);
	}
	
	public boolean hasPoster() {
		return (mPoster != null);
	}
	
	
	public synchronized void setPoster(HttpServletRequest req,
				HttpServletResponse resp) {

		/*
		the req object has an input stream; use getInputStream() to get it.
		Also get the content length as: getContentLength() and type as getContentType().
		*/

		mPoster = new HTTPClientConnection(req,resp);

		System.out.println("Set Poster");
		if(hasGetter()) {
			System.out.println("Poster notifying...");
			this.notify();
		} else {
			while (null == mGetter){
				try {
					System.out.println("Poster waiting...");
					this.wait();
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
			}
		}

		System.out.println("Poster done waiting");
		// only one side bridges connection:
		bridgeConnections();
	}

	public synchronized void setGetter(HttpServletRequest req,
				HttpServletResponse resp) {
		
		System.out.println("Set Getter");
		mGetter = new HTTPClientConnection(req,resp);
		
		if(hasPoster()) {
			System.out.println("Getter notifying...");
			this.notify();
		} else {
			while (null == mPoster){
				try {
					System.out.println("Getter waiting...");
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("Getter done waiting");
	}

	/**
	 * Does the work of actually bridging our connections,
	 * given a GET and POST request.
	 */
	private void bridgeConnections() {
		// bridge poster.req.getInputStream()
			
		// and
		// getter.resp.getOutputStream()

		//need to get header
		//send other header

		//get InputStream
		//send InputStream

		try{
			String postType = mPoster.request.getContentType();
			//int postLength = mPoster.request.getContentLength();			
						
			ServletInputStream in = mPoster.request.getInputStream();		
			ServletOutputStream out = mGetter.response.getOutputStream();

			if(postType.indexOf("multipart/form-data") == -1){
				//mGetter.response.setContentType("application/pdf");
				int i;
				byte[] buf = new byte[BUFFER_SIZE];
				while(-1 != (i = in.read(buf))) {
					out.write(buf,0,i);
				}
			}
			else{
				StringBuffer line = new StringBuffer();
				StringBuffer preamble = new StringBuffer();
				int i = in.read();
				while(i != -1){
					if((char)i == '\n'){
						String s = line.toString();	
						String CONTENT_TYPE = "Content-Type: ";
						if(s.indexOf(CONTENT_TYPE) != -1){
							String type = s.substring(CONTENT_TYPE.length());
							if(!type.equals("multipart/form-data")){
								mGetter.response.setContentType(type);
								preamble.append((char)i);
								break;
							}
						}
						line = new StringBuffer();
					}
					line.append((char)i);
					preamble.append((char)i);
					i = in.read();
				}
				out.print(preamble.toString());
				
				byte[] buf = new byte[BUFFER_SIZE];
				while(-1 != (i = in.read(buf))) {
					out.write(buf,0,i);
				}
			}
			
			
			
			in.close();
			out.close();

		}
		catch (Exception e)
		{
		//print some type of error here.
		}	
	}
	
	class HTTPClientConnection {
		public HttpServletRequest request;
		public HttpServletResponse response;
		
		public HTTPClientConnection(HttpServletRequest req, HttpServletResponse resp) {
			this.request = req;
			this.response = resp;
		}
	}
}
