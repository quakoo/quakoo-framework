package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.framework.ext.push.HuaWeiSender;
import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.service.AndroidHuaWeiPushService;
import com.quakoo.framework.ext.push.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Set;

/**
 * 华为推送处理类
 * class_name: AndroidHuaWeiPushServiceImpl
 * package: com.quakoo.framework.ext.push.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 13:48
 **/
public class AndroidHuaWeiPushServiceImpl extends BaseService implements AndroidHuaWeiPushService,
        InitializingBean {

    Logger logger = LoggerFactory.getLogger(AndroidHuaWeiPushServiceImpl.class);

    private HuaWeiSender sender; //华为推送客户端

    @Override
    public void afterPropertiesSet() throws Exception {
        if(StringUtils.isNotBlank(pushInfo.androidHuaweiPushAppsecret))
            sender = new HuaWeiSender(pushInfo.androidHuaweiPushAppsecret,
                pushInfo.androidHuaweiPushAppid, pushInfo.androidHuaweiPushPackagename, pushInfo.androidHuaweiPushActivity, pushInfo.androidHuaweiPushScheme);
    }

    /**
     * 批量推送
     * method_name: batchPush
     * params: [userInfos, pushMsg]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 13:54
     **/
    @Override
    public void batchPush(List<PushUserInfoPool> userInfos, PushMsg pushMsg) {
        try {
            Set<String> tokens = Sets.newHashSet();
            for(PushUserInfoPool one : userInfos) {
                tokens.add(one.getHuaWeiToken());
            }
            if(null != sender) sender.send(Lists.newArrayList(tokens), pushMsg.getTitle(), pushMsg.getContent(), HuaWeiSender.type_notice,  pushMsg.getExtra());
//            logger.error("===========huawei tokens : "+ tokens.toString() + " pushMsg : " + pushMsg.getTitle());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
