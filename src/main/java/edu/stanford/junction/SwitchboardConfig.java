package edu.stanford.junction;

import edu.stanford.junction.provider.JunctionProvider;

/**
 * A class implementing this interface can be instantiated and loaded by
 * {@link JunctionMaker#getInstance(SwitchboardConfig)}. Subsequent calls to
 * the resulting {@link JunctionMaker} object's {@code newJunction} methods
 * will create {@link Junction} objects using the SwitchboardConfig's associated
 * {@link JunctionProvider} class as a factory.
 */
public interface SwitchboardConfig {}
