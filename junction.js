var JunctionManager = function()
{
    var _lastUser;
    var _chatSubscription;
    var _metaSubscriptions = [];
    var _handshook = false;
    var _connected = false;
    var _cometd;
    var _clientID;
    var _clientChannel;


    return {
        create: function()
        {    
	    if (arguments.length == 2) {
		id = arguments[0];
		cometURL = arguments[1];
	    }
            _clientID = id;
	    _clientChannel = '/junction/client/'+_clientID

            //_cometd = new $.Cometd(); // Creates a new Comet object
            _cometd = $.cometd; // Uses the default Comet object
       	    // Subscribe for meta channels immediately so that the chat knows about meta channel events
   	    _metaSubscribe();
            _cometd.init(cometURL);

            $(window).unload(leave);

    	    return { 
			  chan: 
			  {
				client:_clientChannel,
				join:'join',
				leave:'leave' // maybe these are pseudo-channels?
			  },

			  addListener: function(chan, func) {
				_cometd.subscribe(chan, this, func);
			  },

			  publish: function(chan, msg) {
				_cometd.publish(chan,msg);
			  }
		};
        },

	destroy: function() { leave(); },
    }

    function _chatUnsubscribe()
    {
        if (_chatSubscription) _cometd.unsubscribe(_chatSubscription);
        _chatSubscription = null;
    }

    function _chatSubscribe()
    {
        _chatUnsubscribe();
        _chatSubscription = _cometd.subscribe(_clientChannel, this, receive);
    }

    function _metaUnsubscribe()
    {
        $.each(_metaSubscriptions, function(index, subscription)
        {
            _cometd.removeListener(subscription);
        });
        _metaSubscriptions = [];
    }

    function _metaSubscribe()
    {
        _metaUnsubscribe();
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
                _cometd.startBatch();
                _chatSubscribe();
/*
                _cometd.publish(_clientChannel, {
                    clientID: _clientID,
                    join: true,
                    chat: _clientID + ' has joined'
                });
*/
                _cometd.endBatch();
            }
            else
            {
                // Could not connect
  
            }
        }
    }

    function leave()
    {
        _cometd.startBatch();
        _chatUnsubscribe();
        _cometd.endBatch();

        _metaUnsubscribe();
        _cometd.disconnect();
    }

}();
