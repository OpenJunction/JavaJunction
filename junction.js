var JunctionMaker = function()
{
	var _hostURL;

	function Junction(arg) {
		var _activityDesc = arg;
		var _sessionID = randomUUID(); // TODO: get from activity
		var _hostURL = "http://prpl.stanford.edu:8181/cometd/cometd"; // TODO: get from activity
		var _jm = JunctionManager.create(_hostURL);

		return  {
			  activityDesc : _activityDesc,
			  getSessionID : function() { return _sessionID },

			  sendMessageToChannel: function (channel, msg) {
				_jm.publish(channel,msg);
			  },
			  sendMessageToActor: function (actorID, msg) {
				_jm.publish(_jm.channelForClient(actorID),msg);
			  },
			  sendMessageToRole: function (role, msg) {
				_jm.publish(_jm.channelForRole(role),msg);
			  },
			  sendMessageToSession: function (msg) {
				_jm.publish(_jm.channelForSession(),msg);
			  },

			  getInvitationURL : function () {
				var url = '';
				if (arguments.length == 0) {
					url = _hostURL + "?session="+_sessionID;
				} else if (arguments[0] != false) {
					url = _hostURL + "?session="+_sessionID+"&requestedRole="+arguments[0];
				}
				return url;
			  },
			  getInvitationQR : function () {
				var url;
				var size;
				if (arguments.length == 0) {
					url = _hostURL + "?session="+_sessionID;
				} else if (arguments[0] != false) {
					url = _hostURL + "?session="+_sessionID+"&requestedRole="+arguments[0];
				}
				if (arguments.length == 2) {
					size = arguments[1]+'x'+arguments[1];
				} else {
					size = '250x250';
				}

				return 'http://chart.apis.google.com/chart?cht=qr&chs='+size+'&chl='+encodeURIComponent('{jxref:"'+url+'"}');
				
			  },

			  getActorsForRole : function() { },
			  getRoles : function() { },

			};

	}

	return {
		create: function()
		{
			if (arguments.length != 1) {
				return false;
			}
			_hostURL = arguments[0];

			return {
				newJunction: function()
				{
					if (arguments.length != 1) {
						return false;
					}
					
					return Junction(arguments[0]);
				}
			};
		}
	}
}();


// TODO: Use JQuery to load this script from another file

/* randomUUID.js - Version 1.0
 * 
 * Copyright 2008, Robert Kieffer
 * 
 * This software is made available under the terms of the Open Software License
 * v3.0 (available here: http://www.opensource.org/licenses/osl-3.0.php )
 *
 * The latest version of this file can be found at:
 * http://www.broofa.com/Tools/randomUUID.js
 *
 * For more information, or to comment on this, please go to:
 * http://www.broofa.com/blog/?p=151
 */

/**
 * Create and return a "version 4" RFC-4122 UUID string.
 */

function randomUUID() {
  var s = [], itoh = '0123456789ABCDEF';
  // Make array of random hex digits. The UUID only has 32 digits in it, but we
  // allocate an extra items to make room for the '-'s we'll be inserting.
  for (var i = 0; i <36; i++) s[i] = Math.floor(Math.random()*0x10);

  // Conform to RFC-4122, section 4.4
  s[14] = 4;  // Set 4 high bits of time_high field to version
  s[19] = (s[19] & 0x3) | 0x8;  // Specify 2 high bits of clock sequence

  // Convert to hex chars
  for (var i = 0; i <36; i++) s[i] = itoh[s[i]];

  // Insert '-'s
  s[8] = s[13] = s[18] = s[23] = '-';

  return s.join('');
}



















// TODO: remove this legacy object
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
