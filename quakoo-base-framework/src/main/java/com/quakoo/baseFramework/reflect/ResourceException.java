package com.quakoo.baseFramework.reflect;

/**
 * @author liyongbiao
 *
 */
public class ResourceException extends RuntimeException {

    /** Serial Version ID. */
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new InternalFrameworkException from an Exception.
     * 
     * @param exception
     *            the exception to encapsulate
     */
    public ResourceException(Exception exception) {
        super(exception);
    }

    /**
     * Constructs a new InternalFrameworkException from an exception and a reason.
     * 
     * @param exception
     *            the exception to encapsulate
     * @param reason
     *            the reason for the exception
     */
    public ResourceException(String reason, Exception exception) {
        super(reason, exception);
    }

    /**
     * Constructs a new InternalFrameworkException from a reason.
     * 
     * @param reason
     *            the reason for the exception
     */
    public ResourceException(String reason) {
        super(reason);
    }
}
