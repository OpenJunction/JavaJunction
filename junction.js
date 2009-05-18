var JunctionManager = function()
{
// todo: publish takes 3 params. The last 2 merge to form 1 message.
// use this to maintain certain data (client, etc)
// think about what server should inject and what client should.

    var _lastUser;
    var _subscriptions = [];
    var _metaSubscriptions = [];
    var _handshook = false;
    var _connected = false;
    var _cometd;
    var _clientID;
    var _agentType;
    var _sessionID;
    var _channels = function(ch) {
			return '/'+_sessionID+'/'+ch;			
		     };
	_channels.join='/junction/events/join';
        _channels.leave='/junction/events/leave'; // maybe these are virtual-channels?
	_channels.client=null;
	_channels.session=null;
	_channels.messages=null; // client+session
	_channels.membership=null; // join+leave

    return {
        create: function()
        {    
	    _clientID = 'client_'+Math.floor(Math.random()*10000)
	    _sessionID = 'session_'+Math.floor(Math.random()*10000)

	    if (arguments[0] !== null && typeof(arguments[0]) == 'object'){
		act = arguments[0];
		cometURL=act.host;
		if (act.channels){
			// todo: add channels
		}

		var _agents = [];
		if (act.agents) {
			for (i=0;i<act.agents.length;i++){
				if (typeof(act.agents[i])=='string') {
					_agents.push(act.agents[i]);
				} else {
					_agents.push(act.agents[i].name);
				}
			}
		}

		if (act.sessionID) {
		  _sessionID = act.sessionID;
		}
		if (act.clientID) {
		  _clientID = act.clientID;
		}
		if (act.agentType) {
		  _agentType = act.agentType;
		}

		if (arguments.length >= 2 && typeof(arguments[1]) == 'string') {
		  if (undefined == act.agents || undefined == act.agents[arguments[1]]) {
		    return false;
		  }
		  _agentType = arguments[1];
		}


	    }else{
	    	if (arguments.length == 2) {
			_clientID = arguments[0];
			cometURL = arguments[1];
	    	} else if (arguments.length == 1) {
			cometURL = arguments[0];
	    	} else if (arguments.length == 0) {
			cometURL = document.location.protocol + '//' + document.location.hostname + ':' + document.location.port + '/cometd/cometd';
	    	}
 	    }

	    _channels.session = '/session/'+_sessionID;
	    _channels.client = _channels.session+'/client/'+_clientID
	    _channels.messages = [_channels.session,_channels.client];
	    _channels.membership = [_channels.session+'/events/join',_channels.session+'/events/leave'];

            //_cometd = new $.Cometd(); // Creates a new Comet object
            _cometd = $.cometd; // Uses the default Comet object
       	    // Subscribe for meta channels immediately so that the chat knows about meta channel events
   	    _metaSubscribe();
            _cometd.init(cometURL);
            $(window).unload(leave);

		// todo: server should publish this message
	    _cometd.publish(_channels.join,this,{ clientID: _clientID });

    	    return { 
			  chan: _channels,
			  channelForSession: function() { return _channels.session; },
			  channelForClient: 
				function(c) {
					if (arguments.length == 0)
					  return _channels.client;

					return _channels.session+'/client/'+c;
				},

			  addListener: function() {
				var chan = null;
				var func = null;
				if (arguments.length == 2) {
					chan = arguments[0];
					func = arguments[1];
				} else if (arguments.length == 1) {
					chan = _channels.messages;
					func = arguments[0];
				} else {
					return;
				}

				if (chan instanceof Array) {
					_cometd.startBatch();
					for (var i=0; i<chan.length;i++){
						_subscriptions.push(_cometd.subscribe(chan[i], this, func));
					}
					_cometd.endBatch();
				} else {
					_subscriptions.push(_cometd.subscribe(chan, this, func));
				}
			  },

			  publish: function() {
				var chan = null;
				var msg = null;
				if (arguments.length == 2) {
					chan = arguments[0];
					msg = arguments[1];
				} else if (arguments.length == 1) {
					chan = _channels.session
					msg = arguments[0];
				} else {
					return;
				}
				_cometd.publish(chan,msg);
			  },

			  query: function(target,query,callback) {
				if (typeof(query) == 'string') {
					var q = Object();
					q.queryText = query;
					q.queryType='Unknown';
				} else {
					var q = query;
				}
				var rand= Math.floor(Math.random()*100000);
				var chan = "/private/query_"+rand;
				q.jxMessageType='jxquery';
				q.responseChannel=chan;
				_cometd.subscribe(chan, this, callback);
				_cometd.publish(target,q);
			  }
		};
        },

	destroy: function() { leave(); },
    }

    function _unsubscribe(subscriptions)
    {
        $.each(subscriptions, function(index, subscription)
        {
            _cometd.removeListener(subscription);
        });
        _metaSubscriptions = [];
    }

    function _metaSubscribe()
    {
        _unsubscribe(_metaSubscriptions);
        _metaSubscriptions.push(_cometd.addListener('/meta/handshake', this, _metaHandshake));
        _metaSubscriptions.push(_cometd.addListener('/meta/connect', this, _metaConnect));
    }

    function _metaHandshake(message)
    {
        _handshook = message.successful;
        _connected = false;
    }

    function _metaConnect(message)
    {
        var wasConnected = _connected;
        _connected = message.successful;
        if (wasConnected)
        {
            if (_connected)
            {
                // Normal operation, a long poll that reconnects
            }
            else
            {
                // Disconnected
            }
        }
        else
        {
            if (_connected)
            {
/*
                _cometd.startBatch();
                _chatSubscribe();

                _cometd.publish(_clientChannel, {
                    clientID: _clientID,
                    join: true,
                    chat: _clientID + ' has joined'
                });

                _cometd.endBatch();
*/
            }
            else
            {
                // Could not connect
  
            }
        }
    }

    function leave()
    {
        if (!_clientID) return;

        _cometd.startBatch();
	_unsubscribe(_subscriptions);
        _cometd.publish(_channels.leave,{ clientID: _clientID });
	_cometd.endBatch();

        _unsubscribe(_metaSubscriptions);

        _clientID = null;
        _cometd.disconnect();

    }

}();
