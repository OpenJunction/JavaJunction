package edu.stanford.junction;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A generic exception that is thrown when an error occurs performing an
 * Junction operation. 
 *
 * Based on org.jivesoftware.smack.XMPPException
 *
 * @author Aemon Cannon
 */
public class JunctionException extends Exception {

    private Throwable wrappedThrowable = null;

    /**
     * Creates a new JunctionException.
     */
    public JunctionException() {
        super();
    }

    /**
     * Creates a new JunctionException with a description of the exception.
     *
     * @param message description of the exception.
     */
    public JunctionException(String message) {
        super(message);
    }

    /**
     * Creates a new JunctionException with the Throwable that was the root cause of the
     * exception.
     *
     * @param wrappedThrowable the root cause of the exception.
     */
    public JunctionException(Throwable wrappedThrowable) {
        super();
        this.wrappedThrowable = wrappedThrowable;
    }


    /**
     * Creates a new JunctionException with a description of the exception and the
     * Throwable that was the root cause of the exception.
     *
     * @param message a description of the exception.
     * @param wrappedThrowable the root cause of the exception.
     */
    public JunctionException(String message, Throwable wrappedThrowable) {
        super(message);
        this.wrappedThrowable = wrappedThrowable;
    }

    /**
     * Returns the Throwable asscociated with this exception, or <tt>null</tt> if there
     * isn't one.
     *
     * @return the Throwable asscociated with this exception.
     */
    public Throwable getWrappedThrowable() {
        return wrappedThrowable;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        if (wrappedThrowable != null) {
            out.println("Nested Exception: ");
            wrappedThrowable.printStackTrace(out);
        }
    }

    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        if (wrappedThrowable != null) {
            out.println("Nested Exception: ");
            wrappedThrowable.printStackTrace(out);
        }
    }

    public String getMessage() {
        String msg = super.getMessage();
        return msg;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        String message = super.getMessage();
        if (message != null) {
            buf.append(message).append(": ");
        }
        if (wrappedThrowable != null) {
            buf.append("\n  -- caused by: ").append(wrappedThrowable);
        }
        return buf.toString();
    }
}