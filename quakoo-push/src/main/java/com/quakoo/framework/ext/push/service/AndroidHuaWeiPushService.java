package com.quakoo.framework.ext.push.service;

import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;

import java.util.List;

public interface AndroidHuaWeiPushService {

    public void batchPush(List<PushUserInfoPool> userInfos, Payload payload);

}
