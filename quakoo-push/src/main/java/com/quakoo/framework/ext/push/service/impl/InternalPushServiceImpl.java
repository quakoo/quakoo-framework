package com.quakoo.framework.ext.push.service.impl;


import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.param.InternalPushItem;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.InternalPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.jackson.JsonUtils;

public class InternalPushServiceImpl extends BaseService implements InternalPushService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(InternalPushServiceImpl.class);

    private String channel;

    @Override
    public void afterPropertiesSet() throws Exception {
        channel = pushInfo.projectName + "_internalPush";
    }

    @Resource
    private RedisTemplate<?, ?> redisTemplate;

    @Override
    public void push(long uid, PushMsg pushMsg) {
        InternalPushItem pushItem = new InternalPushItem();
        pushItem.setUids(Lists.newArrayList(uid));
        pushItem.setPushMsg(pushMsg);
        logger.error("===========internal uid : "+ uid + " pushMsg : " + pushMsg.getTitle());
        redisTemplate.convertAndSend(channel, JsonUtils.toJson(pushItem));
    }

    @Override
    public void batchPush(List<Long> uids, PushMsg pushMsg) {
        InternalPushItem pushItem = new InternalPushItem();
        pushItem.setUids(uids);
        pushItem.setPushMsg(pushMsg);
        logger.error("===========internal uids : "+ uids.toString() + " pushMsg : " + pushMsg.getTitle());
        redisTemplate.convertAndSend(channel, JsonUtils.toJson(pushItem));
    }
	
}
