package com.quakoo.framework.ext.push.listener;



import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.context.handle.PushNioHandleContextHandle;
import com.quakoo.framework.ext.push.model.param.InternalPushItem;

/**
 * 内部推送通知监听
 * class_name: InternalPushListener
 * package: com.quakoo.framework.ext.push.listener
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:58
 **/
public class InternalPushListener implements MessageListener  {
	
	private RedisTemplate<?, ?> redisTemplate;

	/**
     * 接收到消息存入到本地队列
	 * method_name: onMessage
	 * params: [msg, arg1]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:59
	 **/
	@Override
	public void onMessage(Message msg, byte[] arg1) {
		byte[] body = msg.getBody();
		String str = redisTemplate.getStringSerializer().deserialize(body);
		InternalPushItem item = JsonUtils.fromJson(str, InternalPushItem.class);
		PushNioHandleContextHandle.push_queue.add(item);
	}

	public RedisTemplate<?, ?> getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(RedisTemplate<?, ?> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	
}
