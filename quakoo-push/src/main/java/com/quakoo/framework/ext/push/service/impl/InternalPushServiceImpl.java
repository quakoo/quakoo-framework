package com.quakoo.framework.ext.push.service.impl;


import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.PushMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.model.param.InternalPushItem;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.InternalPushService;

/**
 * 内部推送处理类
 * class_name: InternalPushServiceImpl
 * package: com.quakoo.framework.ext.push.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 13:59
 **/
public class InternalPushServiceImpl extends BaseService implements InternalPushService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(InternalPushServiceImpl.class);

	private String channel;

    @Override
    public void afterPropertiesSet() throws Exception {
        channel = pushInfo.projectName + "_internalPush";
    }

    @Resource
	private RedisTemplate<?, ?> redisTemplate; //内部消息队列

    /**
     * 推送单个用户
     * method_name: push
     * params: [uid, pushMsg]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:01
     **/
	@Override
	public void push(long uid, PushMsg pushMsg) {
		InternalPushItem pushItem = new InternalPushItem();
		pushItem.setUids(Lists.newArrayList(uid));
		pushItem.setPushMsg(pushMsg);
//        logger.error("===========internal uid : "+ uid + " pushMsg : " + pushMsg.getTitle());
		redisTemplate.convertAndSend(channel, JsonUtils.toJson(pushItem));
	}

	/**
     * 推送多个用户
	 * method_name: batchPush
	 * params: [uids, pushMsg]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 14:01
	 **/
	@Override
	public void batchPush(List<Long> uids, PushMsg pushMsg) {
		InternalPushItem pushItem = new InternalPushItem();
		pushItem.setUids(uids);
		pushItem.setPushMsg(pushMsg);
//        logger.error("===========internal uids : "+ uids.toString() + " pushMsg : " + pushMsg.getTitle());
		redisTemplate.convertAndSend(channel, JsonUtils.toJson(pushItem));
	}
	
}
