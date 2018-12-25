package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

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
	
	public void handle(long historyTime) throws Exception {
		Set<Object> list = cache.zrangeByScoreObject(chatInfo.redis_will_push_queue,
				0, historyTime, null);
		if(list.size() > 0) {
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
			List<Long> mids = Lists.newArrayList();
			for(WillPushItem item : items) {
				long uid = item.getUid();
				long mid = item.getMid();
				long itemFinishTime = item.getTime();
				Long loginTime = userLoginTimeMap.get(uid);
				if(null != loginTime && itemFinishTime > loginTime.longValue()) {
					mids.add(mid);
					pushItems.add(item);
				}
			}
			if(mids.size() > 0 && pushItems.size() > 0) {
				List<Message> messages = messageDao.load(mids);
				Map<Long, Message> messageMap = Maps.newHashMap();
				for(Message message : messages) {
					messageMap.put(message.getId(), message);
				}
				List<PushQueue> pushQueues = Lists.newArrayList();
				for(WillPushItem item : pushItems) {
//					long mid = item.getMid();
//					Message message = messageMap.get(mid);
//					int type = message.getType();
//					String messageContent = message.getContent();
//					String content = null;
//					if(type == Type.type_single_chat || type == Type.type_many_chat) {
//						MessageChat chat = JsonUtils.fromJson(messageContent, MessageChat.class);
//						if(StringUtils.isNotBlank(chat.getWord())) {
//							content = messageContent;
//						} else if(StringUtils.isNotBlank(chat.getPicture())) {
//							content = "您收到了一张图片";
//						} else if(StringUtils.isNotBlank(chat.getVideo())) {
//							content = "您收到了一个视频";
//						}else if(StringUtils.isNotBlank(chat.getVoice())){
//							content = "您收到了一条语音";
//						} else {
//							content = "您收到了一条消息";
//						}
//					} else {
//						content = "您收到了一条通知";
//					}
					PushQueue pushQueue = new PushQueue();
					pushQueue.setUid(item.getUid());
					pushQueue.setMid(item.getMid());
					pushQueue.setStatus(Status.unfinished);
					pushQueue.setTime(System.currentTimeMillis());
					pushQueues.add(pushQueue);
				}
				if(pushQueues.size() > 0) {
					pushQueueDao.insert(pushQueues);
				}
			}
			cache.zremrangeByScore(chatInfo.redis_will_push_queue, 0, historyTime);
		}
	}

}
