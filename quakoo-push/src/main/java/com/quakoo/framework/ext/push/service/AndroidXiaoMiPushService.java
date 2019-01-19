package com.quakoo.framework.ext.push.service;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;

import java.util.List;

public interface AndroidXiaoMiPushService {

    public void push(PushUserInfoPool userInfo, PushMsg pushMsg);

    public void batchPush(List<PushUserInfoPool> userInfos, PushMsg pushMsg);

}
