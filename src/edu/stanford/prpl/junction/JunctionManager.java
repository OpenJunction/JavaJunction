//========================================================================
//Copyright 2007 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package edu.stanford.prpl.junction;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.mortbay.cometd.client.BayeuxClient;
import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.thread.QueuedThreadPool;

import edu.stanford.prpl.junction.api.JunctionAPI;

/**
 * this project doesn't work.
 * Tried the following combinations:
 * JunctionManager.java in host project; cometdchat.jar referenced - SUCCESS
 * JunctionManager.java in host project; junctionmanager.jar referenced - FAIL
 * JunctionManager in junctionmanager.jar - FAIL (this is what we ultimately want)
 * 
 * Note that the successful case still prints the stack trace, but it still works.
 * 
 * 
 */


public class JunctionManager extends AbstractLifeCycle /*implements JunctionAPI*/
{
	public static void main(String[] argv) {
		JunctionManager jm = new JunctionManager("http://prpl.stanford.edu/cometd/cometd");
		jm.addListener(jm.new JunctionListener(){
			@Override
			public void onMessageReceived(Client arg0, Object arg1) {
				System.out.println("got " + arg1.toString());
			}
		});
		jm.publish("Hello, World");
		jm.publish("Hello again, World");
		
		 try {
				Thread.sleep(1500);
			}catch (Exception e){}
	}
	
	
	
    private static String APP_NAME = "junction";
    private HttpClient _httpClient;
    private BayeuxClient _bayeuxClient;
    private QueuedThreadPool _threadPool;
    
    private String _host;
    private int _port;
    private String _uri;
    private String _metaChannel;
    
    private String _publicChannel;
    private String _privateChannel;
    private boolean _loggedIn;

    private boolean _connected = false;
    private String _clientID;
    private String _sessionID;
    
    private Map<String,JunctionListener>_listeners;

    public JunctionManager(String cometServer) // todo: optional sessionID? clientID?
    {
    	_sessionID="mysessID";
    	_publicChannel = "/junction/session/"+_sessionID;
    	try {
    		_clientID = UUID.randomUUID().toString();
    		URL mURL = new URL(cometServer);
    		_port = mURL.getPort();
    		if (_port < 0) {
    			_port = mURL.getDefaultPort();
    		}
    		_host = mURL.getHost();
    		_uri = mURL.getPath();
    		
    		_listeners = new HashMap<String,JunctionListener>();
    		
    	} catch (Exception e) {
    		//Log.e(APP_NAME, "could not parse URL",e);
    	}
    	
    	try {
    		doStart();
    	} catch (Exception e) {
    		//Log.e(APP_NAME,"could not start bayeux",e);	
    	}
    	
    	/*
        this(System.getProperty("chatroom.host", "localhost"), 
                Integer.parseInt(System.getProperty("chatroom.port", "8080")), 
                System.getProperty("chatroom.uri", "/cometd/cometd"), 
                System.getProperty("chatroom.publicChannel", "/chat/demo"),
                System.getProperty("chatroom.privateChannel", "/service/privatechat"),
                System.getProperty("chatroom.metaChannel", "/cometd/meta"));
         */
    }
    
    public String getMetaChannel()
    {
        return _metaChannel;
    }

    public boolean isConnected()
    {
        return _connected;
    }
    
    public void setHost(String host)
    {
        _host = host;
    }
    
    public void setPort(int port)
    {
        _port = port;
    }
    
    public void setUri(String uri)
    {
        _uri = uri;
    }

    public HttpClient getHttpClient()
    {
        return _httpClient;
    }
    
    protected void doStart() throws Exception
    {        
        //Log.i(APP_NAME, "starting chat client.");
        
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
        
        
        //Log.i(APP_NAME, "http client started.");
        if(_bayeuxClient==null)
        {
        	
            _bayeuxClient = new BayeuxClient(_httpClient, new InetSocketAddress(_host, _port), _uri);
            _bayeuxClient.addListener(new MessageRouter());
        }
        
        // start asyncrhonously due to long socket timeout
        // workaround for the android jvm bug when the endpoint doesnt exist        
        _threadPool.dispatch(new Runnable()
        {
            public void run()
            {
                _bayeuxClient.start();
                //Log.i(APP_NAME, "bayeux client started.");
            }
        });
        
        //Log.i(APP_NAME, "chat client started.");
        
        join();
    }
    
    protected void doStop() throws Exception
    {
        //Log.i(APP_NAME, "stopping chat client.");
        
        if(_connected && _loggedIn)
        {
            //Log.i(APP_NAME, "leaving chat room.");
            leave();
            //Log.i(APP_NAME, "removing client from chat room.");
            _bayeuxClient.remove(false);
        }        

       /*CometdChatApplication.dispatch(new Runnable()
        {
            public void run()
            {
                try
                {
                    _httpClient.stop();                    
                    Log.i(getClass().getSimpleName(), "http client stopped.");
                    
                }
                catch(Exception e)
                {
                    Log.i(getClass().getSimpleName(), e.getMessage(), e);
                }    
            }
        });*/        
        //Log.i(APP_NAME, "chat client stopped.");
    }


    
    public boolean join() throws Exception
    {
        if(_loggedIn)
            return false;      
        
        //Log.i(APP_NAME, "joining channels.");        
		
        _bayeuxClient.startBatch();
        
        _bayeuxClient.subscribe(_privateChannel);
/*
        _bayeuxClient.publish(_privateChannel, 
                new Msg().add("clientID", _clientID)
                .add("join", Boolean.TRUE)
                .add("chat", _clientID + " has joined"), 
                String.valueOf(System.currentTimeMillis()));
 */               
        _bayeuxClient.endBatch();
        
        _loggedIn=true;
        
        // hack for now
        try {
			Thread.sleep(150);
		}catch (Exception e){}
    
        return true;
    }    
    
    public boolean leave() throws Exception
    {
        if(!_loggedIn)
            return false;
        
        //Log.i(APP_NAME, "leaving channel: " + _privateChannel + " with " + _clientID);
        
        _bayeuxClient.startBatch();
        
        _bayeuxClient.unsubscribe(_privateChannel);
        /*
        _bayeuxClient.publish(_privateChannel, 
                new Msg().add("clientID", _clientID)
                .add("leave", Boolean.TRUE),
                String.valueOf(System.currentTimeMillis()));        
          */      
        _bayeuxClient.endBatch();
        _loggedIn = false;
        return true;
    }
    
    public boolean publish(Object message) {
    	return publish(_publicChannel,message);
    }
    
    public boolean publish(String channel, Object message)
    {
        if(!_loggedIn)
            return false;

        _bayeuxClient.publish(channel, message, String.valueOf(System.currentTimeMillis()));

        return true;
    }

    
    // TODO: support multiple channel subscriptions
    public void addListener(JunctionListener listener) {
    	_listeners.put(_publicChannel, listener);
    }
    
    public void addListener(String channel, JunctionListener listener) {
    	_bayeuxClient.subscribe(channel);
    	_listeners.put(channel, listener);
    }
    
    public void removeListener(String channel) {
    	if (_listeners.containsKey(channel))
    		_listeners.remove(channel);
    }
    
    /*
     *  Having trouble understanding the purpose of ChannelListener.
     * Using this instead.
     *
     */
    class MessageRouter implements MessageListener
    {

        public void deliver(Client from, Client to, Message message)
        {
        	//Log.d(APP_NAME,"delivering " + message.getData() + " to " + to);
            if(!_connected)
            {
                _connected = true;
                synchronized(this)
                {
                    this.notify();
                }
            }

            /*
            Object data = message.getData();
            if(data==null){
                //Log.d("junction","null data");
                return;
            }*/
            
            if (_listeners.containsKey(message.getChannel())) {
            	_listeners.get(message.getChannel()).onMessageReceived(from, message);
            }
            
        }
    }
    
    /*
    public class channels {
    	String clientChannel = { "/junction/client/"+_clientID };
    	_publicChannel = "/junction/session/"+_sessionID;
    	_metaChannel="cometd/meta";
    	
    }
    */
    
    public abstract class JunctionListener {
    	public abstract void onMessageReceived(Client from, Object data);
    }
    
    public static class Msg extends HashMap<String, Object>
    {
        
        Msg add(String name, Object value)
        {
            put(name, value);
            return this;
        }
        
    }    

}
