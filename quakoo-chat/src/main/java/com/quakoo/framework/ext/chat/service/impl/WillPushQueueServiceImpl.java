package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.model.MessageChat;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.service.ext.ChatPushService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.dao.PushQueueDao;
import com.quakoo.framework.ext.chat.dao.UserInfoDao;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.PushQueue;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.param.WillPushItem;
import com.quakoo.framework.ext.chat.service.WillPushQueueService;

public class WillPushQueueServiceImpl implements WillPushQueueService {
    Logger logger = LoggerFactory.getLogger(WillPushQueueServiceImpl.class);

    @Autowired(required = true)
    @Qualifier("cachePool")
    private JedisX cache;

    @Resource
    private AbstractChatInfo chatInfo;

    @Resource
    private UserInfoDao userInfoDao;

    @Resource
    private MessageDao messageDao;

    @Resource
    private PushQueueDao pushQueueDao;

    @Autowired(required = false)
    @Qualifier("chatPushService")
    private ChatPushService chatPushService;

    public void handle(long historyTime) {
        Set<Object> list = cache.zrangeByScoreObject(chatInfo.redis_will_push_queue,
                0, historyTime, null);
        if(list.size() > 0) {
            try {
                List<WillPushItem> items = Lists.newArrayList();
                for(Object one : list) {
                    items.add((WillPushItem) one);
                }
                Set<Long> uids = Sets.newHashSet();
                for(WillPushItem item : items) {
                    uids.add(item.getUid());
                }
                List<UserInfo> userInfos = userInfoDao.loads(Lists.newArrayList(uids));
                Map<Long, Long> userLoginTimeMap = Maps.newHashMap();
                for(UserInfo userInfo : userInfos) {
                    if(null != userInfo) {
                        userLoginTimeMap.put(userInfo.getUid(), (long) userInfo.getLoginTime());
                    }
                }
                List<WillPushItem> pushItems = Lists.newArrayList();
                for(WillPushItem item : items) {
                    long uid = item.getUid();
                    long itemFinishTime = item.getTime();
                    Long loginTime = userLoginTimeMap.get(uid);
                    if(null != loginTime && itemFinishTime > loginTime.longValue()) {
                        pushItems.add(item);
                    }
                }
                if(pushItems.size() > 0) {
                    Map<Long, List<WillPushItem>> map = Maps.newHashMap();
                    Set<Long> mids = Sets.newHashSet();
                    for(WillPushItem one : pushItems) {
                        long uid = one.getUid();
                        List<WillPushItem> willPushItems = map.get(uid);
                        if(null == willPushItems) {
                            willPushItems = Lists.newArrayList();
                            map.put(uid, willPushItems);
                        }
                        willPushItems.add(one);
                        mids.add(one.getMid());
                    }
                    List<Message> messages = messageDao.load(Lists.newArrayList(mids));
                    Map<Long, Message> messageMap = Maps.newHashMap();
                    for(Message message : messages) {
                        messageMap.put(message.getId(), message);
                    }
                    for(Map.Entry<Long, List<WillPushItem>> entry : map.entrySet()) {
                        long uid = entry.getKey();
                        List<WillPushItem> willPushItems = entry.getValue();
                        int willPushSize = willPushItems.size();
                        if(willPushSize > 1) {
                            chatPushService.sendPush(uid, "收到"+willPushSize+"条新消息");
                        } else {
                            WillPushItem one = willPushItems.get(0);
                            long mid = one.getMid();
                            Message message = messageMap.get(mid);
                            String content = null;
                            if(null != message) {
                                int type = message.getType();
                                String messageContent = message.getContent();
                                if (type == Type.type_single_chat || type == Type.type_many_chat) {
                                    MessageChat chat = JsonUtils.fromJson(messageContent, MessageChat.class);
                                    if (StringUtils.isNotBlank(chat.getWord())) {
                                        content = chat.getWord();
                                    } else if (StringUtils.isNotBlank(chat.getPicture())) {
                                        content = "您收到了一张图片";
                                    } else if (StringUtils.isNotBlank(chat.getVideo())) {
                                        content = "您收到了一个视频";
                                    } else if (StringUtils.isNotBlank(chat.getVoice())) {
                                        content = "您收到了一条语音";
                                    } else {
                                        content = "您收到了一条消息";
                                    }
                                } else {
                                    content = "您收到了一条通知";
                                }
                            } else content = "您收到了一条消息";
                            chatPushService.sendPush(uid, content);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            cache.zremrangeByScore(chatInfo.redis_will_push_queue, 0, historyTime);
        }
    }

}