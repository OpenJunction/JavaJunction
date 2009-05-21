package edu.stanford.prpl.junction.extra;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

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
				int i = in.read();
				while(i != -1){
					out.write((char) i);
					i = in.read();
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
				i = in.read();
				while(i != -1){
					out.write((char)i);
					i = in.read();
				}
			}
			
			//StringBuffer s = new StringBuffer();
			/*int i = inStream.read();
			while(i != -1){
				outStream.print((char)i);
				//s.append((char)i);
				i = inStream.read();
			}*/

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
