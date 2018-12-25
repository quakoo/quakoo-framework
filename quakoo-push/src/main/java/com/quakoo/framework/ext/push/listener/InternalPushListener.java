package com.quakoo.framework.ext.push.listener;



import com.quakoo.framework.ext.push.context.handle.PushNioHandleContextHandle;
import com.quakoo.framework.ext.push.model.param.InternalPushItem;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import com.quakoo.baseFramework.jackson.JsonUtils;

public class InternalPushListener implements MessageListener  {
	
	private RedisTemplate<?, ?> redisTemplate;

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
