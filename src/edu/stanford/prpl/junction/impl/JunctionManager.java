package edu.stanford.prpl.junction.impl;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.cometd.MessageImpl;
import org.mortbay.cometd.client.BayeuxClient;
import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.thread.QueuedThreadPool;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.JunctionCallback;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.JunctionMessage;
import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.api.messaging.JunctionQueryHandler;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;

public class JunctionManager extends AbstractLifeCycle implements JunctionAPI  {
	protected JSONObject mDescriptor; // Activity descriptor
	
	protected String mSessionID;
	protected String mClientID;
	protected Map<String,HashSet<JunctionListener>> mListeners;
	
	private int _port = -1;
	private String _host;
	private String _uri;
	
	private boolean _connected = false;
    private HttpClient _httpClient;
    private BayeuxClient _bayeuxClient;
    private QueuedThreadPool _threadPool;
	
	/**
	 * Constructor
	 */
	public JunctionManager(JSONObject desc) {
		mDescriptor=desc;
		
		
		// Host 
		
		if (!desc.has("host")) {
			throw new IllegalArgumentException("The 'host' field is required for a Junction session.");
		}
		
		try {
			URL bayeuxServer = null;
			bayeuxServer = new URL(desc.getString("host"));
			
			_port = bayeuxServer.getPort();
    		if (_port < 0) {
    			_port = bayeuxServer.getDefaultPort();
    		}
    		_host = bayeuxServer.getHost();
    		_uri = bayeuxServer.getPath();
    		
		} catch (MalformedURLException e) {
			try {
				throw new IllegalArgumentException("Bad host URL (" + desc.getString("host") + ")");
			} catch (JSONException e1) {
				throw new IllegalArgumentException("Host URL not found in JSON descriptor.");
			}
		} catch (JSONException e) {
			throw new IllegalArgumentException("Key not found in JSON descriptor.");
		}
		
		
		// Client, Role and Session 
		
    	try {
    		if (desc.has("sessionID")) {
    			mSessionID=desc.getString("sessionID");
    		} else {
    			mSessionID = UUID.randomUUID().toString();
    		}
    		
    		
    		if (desc.has("clientID")) {
    			mClientID = desc.getString("clientID");
    		} else {
    			mClientID = UUID.randomUUID().toString();
    		}
    		
    		mListeners = new HashMap<String,HashSet<JunctionListener>>();
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	try {
    		start();
    	} catch (Exception e) {
    		//Log.e(APP_NAME,"could not start bayeux",e);	
    	}
		
	}
	
	/**
	 * Session Management
	 */
	public JSONObject getActivityDescriptor() {
		return mDescriptor;
	}
	
	
	/**
	 * Channel reference API
	 */
	public String channelForRole(String role) {
		return "/role/"+role;
	}
	
	public String channelForSession() {
		return "/session/"+mSessionID;
	}
	
	public String channelForClient() {
		return "/client"+mClientID;
	}
	
	public String channelForClient(String client) {
		return "/client"+client;
	}
	
	
	/**
	 * Query API
	 */
	
	// Send
	
	public void query(String target, JunctionQuery query, JunctionCallback callback) {
		
		// If target is single client,
			// if that client is JAVA
				// send direct query
		// etc.
		
		
		// default query mechanism:
		publish(target,query);
	}

	public void query(String target, JunctionQuery query, String channelName) {
		// TODO Auto-generated method stub

	}

	public InboundObjectStream query(String target, JunctionQuery query) {
		// TODO Auto-generated method stub
		return null;
	}

	// Respond
	
	public void registerQueryHandler(final JunctionQueryHandler handler) {
		JunctionListener listener = new JunctionListener() {
			public void onMessageReceived(Client from, Message message) {
				Object data = message.getData();
				
				if (data == null) {
					// System.out.println("null data");
					return;
				}
				
				JunctionQuery query = null;
				try {
					query = (JunctionQuery)JunctionMessage.load((String)data);
				} catch (Exception e) {
					System.out.println("Message is not a query.");
					return;
				}
				
				if (handler.supportsQuery(query)) {
					// TODO:
					// * Create output stream for the handler to put results
					// * Create input stream for querier to get results
					// * bridge them together
					
					// This involves work in the query() method; by default,
					// create a channel that the OutputStream publishes results to
					// and the InputStream reads results from.
					// The channel can be 'created' in the query() method and sent as
					// part of the query.
					
					// Probably have something like a createObjectStream(in, out) method
					// both for direct connections and bayeux; pipe the streams as needed.
					// eh, I don't know, you'll figure it out.
					System.out.println(message);
					handler.handleQuery(query, null /*result*/);
				} else {
					//System.out.println("does not support query");
				}
			}
		};
		
		
		List<String> channels = handler.acceptedChannels();
		if (null == channels) {
			String chan = channelForSession();
			addListener(chan,listener);
			
			//chan = channelForClient();
			//addListener(chan,listener);
		} else {
			for (String chan : channels) {
				addListener(chan,listener);
			}
		}
	}

	
	
	
	
	
	
		  /*___________________*/
		 //					  //
		// Bayeux Management //
	   // 					//
	  /*___________________*/
	
	
	protected void doStart() throws Exception
    {        
        if(_threadPool==null)
        {
            _threadPool = new QueuedThreadPool();
            _threadPool.setMaxThreads(16);
            _threadPool.setDaemon(true);
            _threadPool.setName(getClass().getSimpleName());
            _threadPool.start();
        }        
        
        
        if(_httpClient==null)
        {
            _httpClient = new HttpClient();        
            _httpClient.setConnectorType(HttpClient.CONNECTOR_SOCKET);        
            _httpClient.setMaxConnectionsPerAddress(5);
            _httpClient.setThreadPool(_threadPool);
            _httpClient.start();
        }        
        
        
        if(_bayeuxClient==null)
        {
        	
            _bayeuxClient = new BayeuxClient(_httpClient, new InetSocketAddress(_host, _port), _uri);
            _bayeuxClient.addListener(new MessageRouter());
        }
        
        _bayeuxClient.start();
        
        
        /*
        // start asyncrhonously due to long socket timeout
        // workaround for the android jvm bug when the endpoint doesnt exist        
        _threadPool.dispatch(new Runnable()
        {
            public void run()
            {
                _bayeuxClient.start();
            }
        });
        */
    }
	
	
	
	public boolean publish(Object message) {
		if (message instanceof JunctionMessage) {
			message = ((JunctionMessage)message).toJSON();
		}
		 _bayeuxClient.publish(channelForSession(), message, String.valueOf(System.currentTimeMillis()));
		 return true;
    }
    
    public boolean publish(String channel, Object message)
    {
    	if (message instanceof JunctionMessage) {
    		message = ((JunctionMessage)message).toJSON();
		}
        _bayeuxClient.publish(channel, message, String.valueOf(System.currentTimeMillis()));
        return true;
    }

    
    // TODO: support multiple channel subscriptions
    public void addListener(JunctionListener listener) {
    	String channel = channelForSession();
    	HashSet<JunctionListener>list = mListeners.get(channelForSession());
    	if (list == null) {
    		list = new HashSet<JunctionListener>();
    		mListeners.put(channel, list);
    	}
    	if (list.size() == 0) {
    		_bayeuxClient.subscribe(channel);
    	}
    	
    	list.add(listener);
    }
    
    public void addListener(String channel, JunctionListener listener) {
    	HashSet<JunctionListener>list = mListeners.get(channelForSession());
    	if (list == null) {
    		list = new HashSet<JunctionListener>();
    		mListeners.put(channel, list);
    	}
    	if (list.size() == 0) {
    		_bayeuxClient.subscribe(channel);
    	}
    	
    	list.add(listener);
    }
    
    /**
     * Removes all listeners from a channel.
     * TODO: We may want to rewrite listeners so that
     * they include the channel inherently. (listener.getChannels())
     * @param channel
     */
    public void removeListeners(String channel) {
    	if (!mListeners.containsKey(channel)) return;
    	
    	HashSet<JunctionListener>set = mListeners.get(channel);
    	if (set.size() > 0) {
    		set = new HashSet<JunctionListener>();
    	}
    	
    	_bayeuxClient.unsubscribe(channel);
    }
	
	
	
	
	class MessageRouter implements MessageListener
    {

        public void deliver(Client from, Client to, Message message)
        {

            if(!_connected)
            {
                _connected = true;
                synchronized(this)
                {
                    this.notify();
                }
            }
            
            if (mListeners.containsKey(message.getChannel())) {
            	for (JunctionListener listener : mListeners.get(message.getChannel())) {
            		listener.onMessageReceived(from, message);
            	}
            }
            
        }
    }
	
}
