package com.quakoo.baseFramework.redis.exception;


/**
 * 
 * @author LiYongbiao
 *
 */
public class JedisXException extends RuntimeException {
    public JedisXException(String message) {
        super(message);
    }

    public JedisXException(Throwable cause) {
        super(cause);
    }

    public JedisXException(String message, Throwable cause) {
        super(message, cause);
    }
}
