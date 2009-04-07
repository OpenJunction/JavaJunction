var listener = function()
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
        init: function(id,cometURL)
        {
            _clientID = id;
	    _clientChannel = '/junction/client/'+_clientID
            //_cometd = new $.Cometd(); // Creates a new Comet object
            _cometd = $.cometd; // Uses the default Comet object
       	    // Subscribe for meta channels immediately so that the chat knows about meta channel events
   	    _metaSubscribe();
            _cometd.init(cometURL);

            $(window).unload(leave);
        }
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

                _cometd.publish(_clientChannel, {
                    clientID: _clientID,
                    join: true,
                    chat: _clientID + ' has joined'
                });

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

    function send()
    {
        var phrase = $('#phrase');
        var text = phrase.val();
        phrase.val('');

        if (!text || !text.length) return;

        var colons = text.indexOf('::');
        if (colons > 0)
        {
            _cometd.publish('/service/privatechat', {
                room: _clientChannel, // This should be replaced by the room name
                user: _username,
                chat: text.substring(colons + 2),
                peer: text.substring(0, colons)
            });
        }
        else
        {
            _cometd.publish(_clientChannel, {
                clientID: _clientID,
                chat: text
            });
        }
    }

    function receive(message)
    {
        if (message.data instanceof Array)
        {
            var list = '';
            $.each(message.data, function(index, datum)
            {
                list += datum + '<br />';
            });
            $('#members').html(list);
        }
        else
        {
            var chat = $('#chat');

            var fromUser = message.data.clientID;
            var membership = message.data.join || message.data.leave;
            var text = message.data.chat;
            if (!text) return;

            if (!membership && fromUser == _lastUser)
            {
                fromUser = '...';
            }
            else
            {
                _lastUser = fromUser;
                fromUser += ':';
            }

            if (membership)
            {
                chat.append('<span class=\"membership\"><span class=\"from\">' + fromUser + '&nbsp;</span><span class=\"text\">' + text + '</span></span><br/>');
                _lastUser = '';
            }
            else if (message.data.scope == 'private')
            {
                chat.append('<span class=\"private\"><span class=\"from\">' + fromUser + '&nbsp;</span><span class=\"text\">[private]&nbsp;' + text + '</span></span><br/>');
            }
            else
            {
                //chat.append('<span class=\"from\">' + fromUser + '&nbsp;</span><span class=\"text\">' + text + '</span><br/>');
	// super safe
	eval(text);
            }

            // There seems to be no easy way in jQuery to handle the scrollTop property
            chat[0].scrollTop = chat[0].scrollHeight - chat.outerHeight();
        }
    }
}();
