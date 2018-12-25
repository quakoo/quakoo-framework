package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.service.AndroidXiaoMiPushService;
import com.quakoo.framework.ext.push.service.BaseService;
import com.xiaomi.xmpush.server.Constants;
import com.xiaomi.xmpush.server.Message;
import com.xiaomi.xmpush.server.Sender;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Set;

public class AndroidXiaoMiPushServiceImpl extends BaseService implements AndroidXiaoMiPushService,
        InitializingBean {

    Logger logger = LoggerFactory.getLogger(AndroidXiaoMiPushServiceImpl.class);

    private Sender sender;
    private String packageName;

    private String notifyIdKey;

    @Override
    public void afterPropertiesSet() throws Exception {
        Constants.useOfficial();
        if(StringUtils.isNotBlank(pushInfo.androidXiaomiPushSecretkey))
            sender = new Sender(pushInfo.androidXiaomiPushSecretkey);
        packageName = pushInfo.androidXiaomiPushPackagename;
        notifyIdKey = pushInfo.projectName + "_android_xiaomi_notifyId";
    }

    @Override
    public void push(PushUserInfoPool userInfo, Payload payload) {
        try {
            long uid = userInfo.getUid();
            int notifyId = cache.incr(notifyIdKey).intValue();
            if(notifyId > 9990) {
                notifyId = 0;
                cache.delete(notifyIdKey);
            }
            Message message = new Message.Builder()
                    .title(payload.getTitle()).passThrough(0)
                    .description(payload.getContent())
                    .restrictedPackageName(packageName)
                    .notifyType(1).notifyId(notifyId)
                    .build();
            sender.sendToAlias(message, String.valueOf(uid), 2);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void batchPush(List<PushUserInfoPool> userInfos, Payload payload) {
        try {
            Set<Long> uids = Sets.newHashSet();
            for(PushUserInfoPool one : userInfos) {
                uids.add(one.getUid());
            }
            int notifyId = cache.incr(notifyIdKey).intValue();
            if(notifyId > 9990) {
                notifyId = 0;
                cache.delete(notifyIdKey);
            }
            List<String> alias = Lists.newArrayList();
            for(long uid : uids) {
                alias.add(String.valueOf(uid));
            }
            Message message = new Message.Builder()
                    .title(payload.getTitle()).passThrough(0)
                    .description(payload.getContent())
                    .restrictedPackageName(packageName)
                    .notifyType(1).notifyId(notifyId)
                    .build();
            sender.sendToAlias(message, alias, 2);
            logger.error("===========xiaomi uids : "+ uids.toString() + " payload : " + payload.getTitle());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
