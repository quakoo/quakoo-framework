package com.quakoo.framework.ext.push.service.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.IosPushService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;
import com.quakoo.baseFramework.redis.RedisIncrParam;
import com.quakoo.baseFramework.reflect.ClassloadUtil;

public class IosPushServiceImpl extends BaseService implements IosPushService,
             InitializingBean, DisposableBean {

    Logger logger = LoggerFactory.getLogger(IosPushServiceImpl.class);

    private ApnsService apnsService;

    private String badgeKeyFormat;

    private int poolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;

    @Override
    public void afterPropertiesSet() throws Exception {
        badgeKeyFormat = pushInfo.projectName + "_ios_badge_%d";
        InputStream inputStream = ClassloadUtil.getClassLoader()
                .getResourceAsStream(pushInfo.iosPushCertificateFileName);
        apnsService = APNS.newService().withCert(inputStream, pushInfo.iosPushPassword)
                .withSandboxDestination().asQueued().asPool(poolSize).
                        withNoErrorDetection().build();
    }

    @Override
    public void destroy() throws Exception {
        if(null != apnsService)
            apnsService.stop();
    }

    private boolean verifyToken(String token) {
        if (StringUtils.isBlank(token) || token.length() < 64) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void batchPush(List<PushUserInfoPool> userInfos, PushMsg pushMsg) {
        for(Iterator<PushUserInfoPool> it = userInfos.iterator(); it.hasNext();) {
            PushUserInfoPool userInfo = it.next();
            if(!verifyToken(userInfo.getIosToken())) it.remove();
        }

        Map<Long, List<String>> uidTokenMap = Maps.newLinkedHashMap();
        for(PushUserInfoPool one : userInfos) {
            long uid = one.getUid();
            String token = one.getIosToken();
            List<String> tokens = uidTokenMap.get(uid);
            if(null == tokens) {
                tokens = Lists.newArrayList();
                uidTokenMap.put(uid, tokens);
            }
            tokens.add(token);
        }

        List<RedisIncrParam> redisIncrParams = Lists.newArrayList();
        for(long uid : uidTokenMap.keySet()) {
            RedisIncrParam param = new RedisIncrParam();
            param.setKey(String.format(badgeKeyFormat, uid));
            redisIncrParams.add(param);
        }
        Map<RedisIncrParam, Long> map = cache.pipIncr(redisIncrParams);
        Map<String, Long> badgeMap = Maps.newHashMap();
        for(Entry<RedisIncrParam, Long> entry : map.entrySet()) {
            String key = entry.getKey().getKey();
            long badge = entry.getValue();
            badgeMap.put(key, badge);
        }

        for(Entry<Long, List<String>> entry : uidTokenMap.entrySet()) {
            long uid = entry.getKey();
            try {
                long badge = badgeMap.get(String.format(badgeKeyFormat, uid));
                PayloadBuilder payloadBuilder = APNS.newPayload().badge((int)badge).
                        alertBody(pushMsg.getTitle() + "\n" + pushMsg.getContent());
                if (payloadBuilder.isTooLong()) {
                    payloadBuilder = payloadBuilder.shrinkBody();
                }
                payloadBuilder = payloadBuilder.forNewsstand();
                Map<String, String> customFields = Maps.newHashMap();
                customFields.put("id", String.valueOf(pushMsg.getId()));
                if (null != pushMsg.getExtra() && pushMsg.getExtra().size() > 0) {
                    customFields.putAll(pushMsg.getExtra());
                }
                payloadBuilder.sound("default");
                payloadBuilder.customFields(customFields);
                String payloadStr = payloadBuilder.build();
                Date expiry = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
                List<String> tokens = entry.getValue();
                apnsService.push(tokens, payloadStr, expiry);
                logger.error("===========ios token : "+ tokens.toString() + " pushMsg : " + pushMsg.getTitle());
            } catch (Exception e) {
                cache.decr(String.format(badgeKeyFormat, uid));
                logger.error(e.getMessage(), e);
            }
        }

    }

    @Override
    public void push(PushUserInfoPool userInfo, PushMsg pushMsg) {
        long uid = userInfo.getUid();
        String token = userInfo.getIosToken();
        if(verifyToken(token)) {
            long badge = cache.incr(String.format(badgeKeyFormat, uid));
            try {
                PayloadBuilder payloadBuilder = APNS.newPayload().badge((int)badge).
                        alertBody(pushMsg.getTitle() + "\n" + pushMsg.getContent());
                if (payloadBuilder.isTooLong()) {
                    payloadBuilder = payloadBuilder.shrinkBody();
                }
                payloadBuilder = payloadBuilder.forNewsstand();

                Map<String, String> customFields = Maps.newHashMap();
                customFields.put("id", String.valueOf(pushMsg.getId()));
                if (null != pushMsg.getExtra() && pushMsg.getExtra().size() > 0) {
                    customFields.putAll(pushMsg.getExtra());
                }
                payloadBuilder.sound("default");
                payloadBuilder.customFields(customFields);
                String payloadStr = payloadBuilder.build();
                Date expiry = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
                apnsService.push(token, payloadStr, expiry);
                logger.error("===========ios token : "+ token + " pushMsg : " + pushMsg.getTitle());
            } catch (Exception e) {
                cache.decr(String.format(badgeKeyFormat, uid));
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean clearBadge(long uid) {
        long ret = cache.delete(String.format(badgeKeyFormat, uid));
        return ret > 0 ? true : false;
    }
	
}
