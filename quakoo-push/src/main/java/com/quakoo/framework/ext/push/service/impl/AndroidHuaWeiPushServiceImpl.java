package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.framework.ext.push.HuaWeiSender;
import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.service.AndroidHuaWeiPushService;
import com.quakoo.framework.ext.push.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Set;

public class AndroidHuaWeiPushServiceImpl extends BaseService implements AndroidHuaWeiPushService,
        InitializingBean {

    Logger logger = LoggerFactory.getLogger(AndroidHuaWeiPushServiceImpl.class);

    private HuaWeiSender sender;

    @Override
    public void afterPropertiesSet() throws Exception {
        if(StringUtils.isNotBlank(pushInfo.androidHuaweiPushAppsecret))
            sender = new HuaWeiSender(pushInfo.androidHuaweiPushAppsecret,
                    pushInfo.androidHuaweiPushAppid, pushInfo.androidHuaweiPushPackagename, pushInfo.androidHuaweiPushActivity, pushInfo.androidHuaweiPushScheme);
    }

    @Override
    public void batchPush(List<PushUserInfoPool> userInfos, Payload payload) {
        try {
            Set<String> tokens = Sets.newHashSet();
            for(PushUserInfoPool one : userInfos) {
                tokens.add(one.getHuaWeiToken());
            }
            sender.send(Lists.newArrayList(tokens), payload.getTitle(), payload.getContent(), HuaWeiSender.type_notice,  payload.getExtra());
            logger.error("===========huawei tokens : "+ tokens.toString() + " payload : " + payload.getTitle());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
