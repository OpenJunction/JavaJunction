package edu.stanford.prpl.junction.extra;

import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpBridge extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public static void main(String[] argv) {

	}
	
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
		} else {
			pair = bridge.get(uri);
		}
		
		if (pair.hasGetter()) {
			// throw error
			return;
		}
		
		pair.setGetter(req, resp);
	}



}

class HttpBridgePair {
	private HTTPClientConnection mPoster;
	private HTTPClientConnection mGetter;
	
	public boolean hasGetter() {
		return (mGetter == null);
	}
	
	public boolean hasPoster() {
		return (mPoster == null);
	}
	
	
	public synchronized void setPoster(HttpServletRequest req,
				HttpServletResponse resp) {

/*
the req object has an input stream; use getInputStream() to get it.
Also get the content length as: getContentLength() and type as getContentType().

*/

		mPoster = new HTTPClientConnection(req,resp);
		
		while (null == mGetter){
			try {
				this.wait();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
		
		bridgeConnections();

	}

	public synchronized void setGetter(HttpServletRequest req,
				HttpServletResponse resp) {
		
		mGetter = new HTTPClientConnection(req,resp);
		
		if(null != mPoster) {
			this.notify();
		}

	}

	
	
	/**
	 * Does the work of actually bridging our connections,
	 * given a GET and POST request.
	 */
	private void bridgeConnections() {
		// bridge poster.req.getInputStream()
		// and
		// getter.resp.getOutputStream()
		
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
