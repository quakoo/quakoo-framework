package com.quakoo.framework.ext.push.service.impl;

import com.quakoo.framework.ext.push.service.LocalCacheService;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class LocalCacheServiceImpl implements LocalCacheService, InitializingBean, DisposableBean {

    private CacheManager cacheManager;
    private Cache cache;

    @Override
    public void afterPropertiesSet() throws Exception {
        Configuration configuration = new Configuration().cache(new CacheConfiguration("local", 20000)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                .timeToIdleSeconds(60 * 60 * 24)
                .timeToLiveSeconds(60 * 60 * 24)
                .eternal(false));
        cacheManager = CacheManager.create(configuration);
        cache = cacheManager.getCache("local");
    }

    @Override
    public void set(String key, Object value) {
        Element element = new Element(key, value);
        cache.put(element);
    }

    @Override
    public Object get(String key) {
        Element element = cache.get(key);
        if(element != null) return element.getObjectValue();
        return null;
    }

    @Override
    public void setString(String key, String value) {
        Element element = new Element(key, value);
        cache.put(element);
    }

    @Override
    public String getString(String key) {
        Element element = cache.get(key);
        if(element != null) return String.valueOf(element.getObjectValue());
        return null;
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void destroy() throws Exception {
        cacheManager.shutdown();
    }
}
