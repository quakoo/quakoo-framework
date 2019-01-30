package com.quakoo.framework.ext.push.service;

import com.quakoo.framework.ext.push.model.PushMsg;

import java.util.List;

public interface InternalPushService {

	public void push(long uid, PushMsg pushMsg);
	
	public void batchPush(List<Long> uids, PushMsg pushMsg);
	
}
