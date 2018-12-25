package com.quakoo.framework.ext.push.service;

import java.util.List;

import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;

public interface IosPushService {

	public void push(PushUserInfoPool userInfo, Payload payload);
	
	public void batchPush(List<PushUserInfoPool> userInfos, Payload payload);
	
	public boolean clearBadge(long  uid);
	
}
