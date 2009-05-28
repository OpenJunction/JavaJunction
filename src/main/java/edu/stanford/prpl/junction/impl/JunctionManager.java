package edu.stanford.prpl.junction.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.server.MessageImpl;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import edu.stanford.prpl.junction.api.JunctionAPI;
import edu.stanford.prpl.junction.api.messaging.JunctionListener;
import edu.stanford.prpl.junction.api.messaging.JunctionMessage;
import edu.stanford.prpl.junction.api.messaging.JunctionQuery;
import edu.stanford.prpl.junction.api.messaging.JunctionQueryHandler;
import edu.stanford.prpl.junction.api.object.InboundObjectStream;
import edu.stanford.prpl.junction.api.object.OutboundObjectStream;
import edu.stanford.prpl.junction.impl.object.BayeuxInboundObjectStream;
import edu.stanford.prpl.junction.impl.object.BayeuxOutboundObjectStream;

public class JunctionManager implements JunctionAPI  {
	protected Map<String,Object> mDescriptor; // Activity descriptor
	
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
	public JunctionManager(Map<String,Object> desc) {
		mDescriptor=desc;
		
		
		// Host 
		
		if (!desc.containsKey("host")) {
			throw new IllegalArgumentException("The 'host' field is required for a Junction session.");
		}
		
		try {
			URL bayeuxServer = null;
			bayeuxServer = new URL((String)desc.get("host"));
			
			_port = bayeuxServer.getPort();
    		if (_port < 0) {
    			_port = bayeuxServer.getDefaultPort();
    		}
    		_host = bayeuxServer.getHost();
    		_uri = bayeuxServer.getPath();
    		
		} catch (MalformedURLException e) {
			try {
				throw new IllegalArgumentException("Bad host URL (" + (String)desc.get("host") + ")");
			} catch (Exception e1) {
				throw new IllegalArgumentException("Host URL not found in descriptor.");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Key not found in JSON descriptor.");
		}
		
		
		// Client, Role and Session 
		
    	try {
    		if (desc.containsKey("sessionID")) {
    			mSessionID=(String)desc.get("sessionID");
    		} else {
    			mSessionID = UUID.randomUUID().toString();
    		}
    		
    		
    		if (desc.containsKey("clientID")) {
    			mClientID = (String)desc.get("clientID");
    		} else {
    			mClientID = UUID.randomUUID().toString();
    		}
    		
    		mListeners = new HashMap<String,HashSet<JunctionListener>>();
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	try {
    		doStart();
    	} catch (Exception e) {
    		//Log.e(APP_NAME,"could not start bayeux",e);	
    	}
		
	}
	
	/**
	 * Session Management
	 */
	public Map<String,Object> getActivityDescriptor() {
		return mDescriptor;
	}
	
	
	/**
	 * Channel reference API
	 */
	public String channelForRole(String role) {
		return "/session/"+mSessionID+"/role/"+role;
	}
	
	public String channelForSession() {
		return "/session/"+mSessionID;
	}
	
	public String channelForClient() {
		return "/session/"+mSessionID+"/client/"+mClientID;
	}
	
	public String channelForClient(String client) {
		return "/session/"+mSessionID+"/client/"+client;
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
		Map<String,Object>map=null;
		try {
			map = query.getMap();
			String responseChannel = "/private/"+UUID.randomUUID().toString();
			map.put("responseChannel", responseChannel);
			
			InboundObjectStream stream = new BayeuxInboundObjectStream(this, responseChannel);
			callback.bind(stream);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		publish(target,map);
	}

	public void query(String target, JunctionQuery query, String channelName) {
		Map<String,Object>map=null;
		try {
			map = query.getMap();
			map.put("responseChannel", channelName);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		publish(target,map);

	}

	public InboundObjectStream query(String target, JunctionQuery query) {
		InboundObjectStream inStream = null;
		
		Map<String,Object>map=null;
		try {
			map=query.getMap();
			String responseChannel = "/private/"+UUID.randomUUID().toString();
			map.put("responseChannel", responseChannel);
			
			inStream = new BayeuxInboundObjectStream(this, responseChannel);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		publish(target,map);
		return inStream;
	}

	// Respond
	
	public void registerQueryHandler(final JunctionQueryHandler handler) {
		JunctionListener listener = new JunctionListener() {
			
			public void onMessageReceived(final Client from, final Message message) {
				
				
				// Not entirely sure why this needs a new thread,
				// since everything else is asynchronous..
				// but this fixes an issue with the query handler blocking.
				new Thread() {
					public void run() {
						Object data = message.getData();
						if (data == null) {
							// System.out.println("null data");
							return;
						}
						
						/*
						JunctionQuery query = null;
						try {
							query = (JunctionQuery)JunctionMessage.load((Map)data);
						} catch (Exception e) {
							System.out.println("Message is not a query.");
							e.printStackTrace();
							return;
						}*/
						
						if (!(message instanceof JunctionMessage)) {
							System.out.println("Message is not a query.");
							return;
						}
							JunctionQuery query = (JunctionQuery)message;
							
						if (handler.supportsQuery(query)) {
							String responseChannel;
							try {
								Map<String,Object> map = (Map<String,Object>)data;
								responseChannel = (String)map.get("responseChannel");
							} catch (Exception e) {
								e.printStackTrace();
								return;
							}
							
							OutboundObjectStream outStream
								= new BayeuxOutboundObjectStream(JunctionManager.this,responseChannel);
							
							
							handler.handleQuery(query, outStream);
							
							
						} else {
							//System.out.println("does not support query");
						}
						
					}
				}.start();
			}
		};
		
		
		List<String> channels = handler.acceptedChannels();
		if (null == channels || channels.size() == 0) {
			String chan = channelForSession();
			addListener(chan,listener);
			
			chan = channelForClient();
			addListener(chan,listener);
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
        	
            _bayeuxClient = new BayeuxClient(_httpClient, new Address(_host, _port), _uri);
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
			message = ((JunctionMessage)message).getMap();
		}
		
		_bayeuxClient.publish(channelForSession(), message, String.valueOf(System.currentTimeMillis()));
		return true;
    }
    
    public boolean publish(String channel, Object message)
    {
    	if (message instanceof JunctionMessage) {
			message = ((JunctionMessage)message).getMap();
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
    	HashSet<JunctionListener>list = mListeners.get(channel);
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
            
            if (JunctionMessage.isJunctionMessage(message)) {
            	message = JunctionMessage.load(message);
            }
            
            if (mListeners.containsKey(message.getChannel())) {
            	for (JunctionListener listener : mListeners.get(message.getChannel())) {
            		listener.onMessageReceived(from, message);
            	}
            }
            
        }
    }
	
}
