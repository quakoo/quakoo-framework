package com.quakoo.framework.ext.chat.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import com.google.common.collect.Sets;
import com.quakoo.framework.ext.chat.dao.UserInfoDao;
import com.quakoo.framework.ext.chat.model.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.MessageChat;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.dao.PushQueueDao;
import com.quakoo.framework.ext.chat.model.PushQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.PushQueueService;
import com.quakoo.framework.ext.chat.service.ext.ChatPushService;


public class PushQueueServiceImpl implements PushQueueService {
	
	Logger logger = LoggerFactory.getLogger(PushQueueServiceImpl.class);

	@Resource
	private PushQueueDao pushQueueDao;
	
	@Autowired(required = false)
    @Qualifier("chatPushService")
	private ChatPushService chatPushService;

	@Resource
	private UserInfoDao userInfoDao;

	@Resource
	private MessageDao messageDao;

    //	public boolean unfinishedIsNull() throws Exception {
//		return pushQueueDao.list_null(Status.unfinished);
//	}
//
//	public List<PushQueue> unfinishedList(int size) throws Exception {
//		return pushQueueDao.all_list(Status.unfinished, size);
//	}
//
//	public List<PushQueue> finishedList(long maxTime, int size)
//			throws Exception {
//		return pushQueueDao.list_time(Status.finished, maxTime, size);
//	}
//
//	public boolean updateStatus(PushQueue one, int newStatus) throws Exception {
//		return pushQueueDao.update(one, newStatus);
//	}

	@Override
	public void handle(int size) throws Exception {
		boolean sign = pushQueueDao.list_null(Status.unfinished);
		if(!sign) {
			List<PushQueue> list = pushQueueDao.all_list(Status.unfinished, size);
			if(null != list && list.size() > 0) {
				Map<Long, List<PushQueue>> map = Maps.newHashMap();
                Set<Long> uids = Sets.newHashSet();
				for(PushQueue one : list) {
					long uid = one.getUid();
					List<PushQueue> pushList = map.get(uid);
					if(null == pushList) {
						pushList = Lists.newArrayList();
						map.put(uid, pushList);
					}
					pushList.add(one);
					uids.add(uid);
				}
				Map<Long, UserInfo> userInfoMap = Maps.newHashMap();
				if(uids.size() > 0) {
				    List<UserInfo> userInfos = userInfoDao.loads(Lists.newArrayList(uids));
				    for(UserInfo userInfo : userInfos) {
				        userInfoMap.put(userInfo.getUid(), userInfo);
                    }
                }
				if(map.size() > 0) {
					for(Entry<Long, List<PushQueue>> entry : map.entrySet()) {
						long uid = entry.getKey();
						try {
                            UserInfo userInfo = userInfoMap.get(uid);
                            if(null != userInfo) {
                                List<PushQueue> pushList = entry.getValue();
                                for(Iterator<PushQueue> it = pushList.iterator(); it.hasNext();) {
                                    PushQueue one = it.next();
                                    if(one.getTime() <= (long)userInfo.getLoginTime()) {
                                        it.remove();
                                    }
                                }
                                int pushSize = pushList.size();
                                if(pushSize > 0) {
                                    if(pushSize > 1) {
                                        chatPushService.sendPush(uid, "收到"+pushSize+"条新消息");
                                    } else {
                                        PushQueue one = pushList.get(0);
                                        long mid = one.getMid();
                                        Message message = messageDao.load(mid);
                                        int type = message.getType();
                                        String messageContent = message.getContent();
                                        String content = null;
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
                                        chatPushService.sendPush(uid, content);
                                    }
                                }
                            }
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
					}
				}
				if(list.size() > 0) {
				    pushQueueDao.update(list, Status.unfinished, Status.finished);
                }
//				for(PushQueue one : list) {
//					pushQueueDao.update(one, Status.finished);
//				}
			}
		}
	}

}
