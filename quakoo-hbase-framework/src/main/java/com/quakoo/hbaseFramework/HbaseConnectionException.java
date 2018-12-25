package com.quakoo.hbaseFramework;

/**
 * Created by 136249 on 2015/3/16.
 */
public class HbaseConnectionException extends RuntimeException {
    public HbaseConnectionException() {
    }

    public HbaseConnectionException(String message) {
        super(message);
    }

    public HbaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public HbaseConnectionException(Throwable cause) {
        super(cause);
    }

    public HbaseConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
