package edu.stanford.junction.provider.jvm;

import edu.stanford.junction.SwitchboardConfig;

/**
 * A Switchboard for actors run within a single JVM.
 * This is mainly a demonstration of how to write
 * different Switchboard implementations.
 * 
 * Someday, it may be useful in conjunction with
 * other Switchboards, in building a hybridized
 * communication network.
 * 
 * @author bjdodson
 *
 */
public class JVMSwitchboardConfig implements SwitchboardConfig {
	/* an email..

 
I had a little fun tonight and wrote a new Switchboard implementation-- JVMSwitchboard. It's used to send messages to actors located within a single JVM. The implementation basically has a set of actors in a session, and routes the JSON message to all actors.

It will be quite buggy, for example:
 * all actors share the mutable JSONObject
 * extras don't currently work
 * onActivityCreate not triggered
 * many more

Mainly its a proof of concept for how to write other switchboards, and it exposed a lot of things I am really not happy with in the API. So if I get time and energy (NOT LIKELY!) I would like to work out those API hickups. For example:
 * Provider implementation shouldn't see so much of the Extras API
 * More clarity with how sessionIDs are created
 * much more

A fun thing it exposes though is how cool it would be to have a network of switchboards. So we have a JVMSwitchboard, LocalhostSwitchboard, OpenflowSwitchboard, and XMPPSwitchboard. The system then auto-figures out which switchboard or switchboards to use to glue together a set of actors. If all clients are on the same box, use LocalhostSwitchboard. If on the same network, OpenflowSwitchboard. If a remote actor joins, cut over to XMPPSwitchboard. If two are on the same network and one remote, federate between OpenflowSwitchboard and XMPPSwitchboard.

This would be hard, but I think PHDs are supposed to be hard ;)


	 */
}
