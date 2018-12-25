package com.quakoo.baseFramework.redis.exception;

import redis.clients.jedis.exceptions.JedisDataException;


/**
 * 
 * @author LiYongbiao
 *
 */
public class JedisXValueNotSupportException extends JedisDataException {
    public JedisXValueNotSupportException(String message) {
        super(message);
    }

    public JedisXValueNotSupportException(Throwable cause) {
        super(cause);
    }

    public JedisXValueNotSupportException(String message, Throwable cause) {
        super(message, cause);
    }
}
