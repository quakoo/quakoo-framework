package com.quakoo.framework.ext.push.service;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.push.AbstractPushInfo;

public class BaseService {

	@Autowired(required = true)
    @Qualifier("cachePool")
    protected JedisX cache;
    
    @Resource
    protected AbstractPushInfo pushInfo;
	
}
