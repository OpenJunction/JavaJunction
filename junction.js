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
    var _channels = {
			join:'/junction/events/join',
			leave:'/junction/events/leave', // maybe these are pseudo-channels?
			client: null,
			session: null,
			messages: null, // client+session
			membership: null, // join+leave

			
		     }

    return {
        create: function()
        {    
	    _clientID = 'client_'+Math.floor(Math.random()*10000)
	    if (arguments.length == 2) {
		_clientID = arguments[0];
		cometURL = arguments[1];
	    } else if (arguments.length == 1) {
		cometURL = arguments[0];
	    } else if (arguments.length == 0) {
		cometURL = document.location.protocol + '//' + document.location.hostname + ':' + document.location.port + '/cometd/cometd';
	    }
 
	    _channels.client = '/junction/client/'+_clientID
	    _channels.session = '/junction/session/mysessID';
	    _channels.messages = ['/junction/client/'+_clientID,'/junction/session/mysessID'];
	    _channels.membership = ['/junction/events/join','/junction/events/leave'];

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
			  
			  client: function(a) { return '/junction/client/'+a; },


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

			  query: function(query) {
				var chan = "/junction/query/response";
				//_cometd.subscribe(chan, this, function() { return
			  },

			  queries: function(queries, settings) {
/*
				function cb(result, callback) {
					if(settings.mode=="FIRST_RESPONSE"){

					}
				}
				for (var i=0;i<queries.length;i++) {
					setTimeout(_query(queries[i].query, queries[i].callback, cb),0);
				}
*/
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
