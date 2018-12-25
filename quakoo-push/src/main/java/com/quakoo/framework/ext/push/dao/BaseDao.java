package com.quakoo.framework.ext.push.dao;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.AbstractPushInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import com.quakoo.baseFramework.redis.JedisX;

public class BaseDao {

	@Resource
	protected JdbcTemplate jdbcTemplate;
    
    @Autowired(required = true)
    @Qualifier("cachePool")
    protected JedisX cache;
    
    @Resource
    protected AbstractPushInfo pushInfo;
	
}
