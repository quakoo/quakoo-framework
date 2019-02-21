package com.quakoo.framework.ext.push.service;

public interface LocalCacheService {

    public void set(String key, Object value);

    public Object get(String key);

    public void setString(String key, String value);

    public String getString(String key);


    public void remove(String key);
}
