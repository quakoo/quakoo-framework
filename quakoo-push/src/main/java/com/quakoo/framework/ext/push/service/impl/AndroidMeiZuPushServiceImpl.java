package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.meizu.push.sdk.server.IFlymePush;
import com.meizu.push.sdk.server.constant.ResultPack;
import com.meizu.push.sdk.server.model.push.PushResult;
import com.meizu.push.sdk.server.model.push.VarnishedMessage;
import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.service.AndroidMeiZuPushService;
import com.quakoo.framework.ext.push.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

public class AndroidMeiZuPushServiceImpl extends BaseService implements AndroidMeiZuPushService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(AndroidMeiZuPushServiceImpl.class);

    private IFlymePush push;

    private long appId;

    @Override
    public void afterPropertiesSet() throws Exception {
        if(StringUtils.isNotBlank(pushInfo.androidMeizuPushAppsecret))
            push = new IFlymePush(pushInfo.androidMeizuPushAppsecret);
        appId = Long.parseLong(pushInfo.androidMeizuPushAppid);
    }

    @Override
    public void batchPush(List<PushUserInfoPool> userInfos, PushMsg pushMsg) {
        try {
            VarnishedMessage.Builder messageBuilder = new VarnishedMessage.Builder().appId(appId)
                    .title(pushMsg.getTitle()).content(pushMsg.getContent());
            for(Map.Entry<String, String> entry : pushMsg.getExtra().entrySet()) {
                messageBuilder.extra(entry.getKey(), entry.getValue());
            }
            VarnishedMessage message = messageBuilder.build();
            List<String> pushIds = Lists.newArrayList();
            for(PushUserInfoPool userInfo : userInfos) {
                pushIds.add(userInfo.getMeiZuPushId());
            }
            if(null != push) push.pushMessage(message, pushIds);
            logger.error("===========meizu pushids : "+ pushIds.toString() + " pushMsg : " + pushMsg.getTitle());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws Exception {
        IFlymePush push  = new IFlymePush("7d7503f647b446b580bbcaa0e947716c");
        VarnishedMessage.Builder messageBuilder = new VarnishedMessage.Builder().appId(117204l)
                .title("2").content("222");
//        for(Map.Entry<String, String> entry : payload.getExtra().entrySet()) {
//            messageBuilder.extra(entry.getKey(), entry.getValue());
//        }
        VarnishedMessage message = messageBuilder.build();
        List<String> pushIds = Lists.newArrayList("Z9K487e027e007d7d637754416543005d09417f047a0d");
        ResultPack<PushResult> res = push.pushMessage(message, pushIds,2);
        System.out.println(res.toString());
    }

}
