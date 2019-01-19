package com.quakoo.framework.ext.push.service;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;

import java.util.List;

public interface AndroidHuaWeiPushService {

    public void batchPush(List<PushUserInfoPool> userInfos, PushMsg pushMsg);

}
