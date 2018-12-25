package com.quakoo.framework.ext.push.service;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.AbstractPushInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.quakoo.baseFramework.redis.JedisX;

public class BaseService {

	@Autowired(required = true)
    @Qualifier("cachePool")
    protected JedisX cache;
    
    @Resource
    protected AbstractPushInfo pushInfo;
	
}
