package com.quakoo.framework.ext.push.service;

import java.util.List;

import com.quakoo.framework.ext.push.model.Payload;

public interface InternalPushService {

	public void push(long uid, Payload payload);
	
	public void batchPush(List<Long> uids, Payload payload);
	
}
