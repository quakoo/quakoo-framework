package com.quakoo.framework.ext.push.service;

import java.util.List;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;

public interface IosPushService {

    public void push(PushUserInfoPool userInfo, PushMsg pushMsg);

    public void batchPush(List<PushUserInfoPool> userInfos, PushMsg pushMsg);

    public boolean clearBadge(long  uid);
	
}
