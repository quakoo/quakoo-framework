package com.quakoo.baseFramework.lock;

/**
 * Created by 136249 on 2015/4/9.
 */
public class ZKLockTimeOutException extends RuntimeException {
    public ZKLockTimeOutException() {
    }

    public ZKLockTimeOutException(String message) {
        super(message);
    }

    public ZKLockTimeOutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZKLockTimeOutException(Throwable cause) {
        super(cause);
    }

    public ZKLockTimeOutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
