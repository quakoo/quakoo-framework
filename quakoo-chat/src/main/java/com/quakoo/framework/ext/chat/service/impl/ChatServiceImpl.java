package com.quakoo.framework.ext.chat.service.impl;

import java.util.Map;

import javax.annotation.Resource;

import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.dao.ChatGroupDao;
import com.quakoo.framework.ext.chat.dao.ManyChatQueueDao;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.dao.SingleChatQueueDao;
import com.quakoo.framework.ext.chat.dao.UserClientInfoDao;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.MessageChat;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import com.quakoo.framework.ext.chat.model.UserClientInfo;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.model.ext.RecallExt;
import com.quakoo.framework.ext.chat.service.ChatService;

public class ChatServiceImpl implements ChatService {
	
	@Resource
	private MessageDao messageDao;
	
	@Resource
	private UserClientInfoDao userClientInfoDao;

	@Resource
	private SingleChatQueueDao singleChatQueueDao;
	
	@Resource
	private ManyChatQueueDao manyChatQueueDao;
	
	@Resource
	private ChatGroupDao chatGroupDao;

	private boolean _insert(long uid, String clientId, int type, long thirdId, String content) {
        boolean res = false;
	    Message message = new Message();
        message.setAuthorId(uid);
        message.setClientId(clientId);
        message.setType(type);
        message.setContent(content);
        message = messageDao.insert(message);
        long mid = message.getId();
        UserClientInfo clientInfo = new UserClientInfo();
        clientInfo.setUid(uid);
        clientInfo.setType(type);
        clientInfo.setThirdId(thirdId);
        clientInfo.setMid(mid);
        clientInfo.setClientId(clientId);
        clientInfo.setCtime(System.currentTimeMillis());
        res = userClientInfoDao.insert(clientInfo);
        if(res) {
            if(Type.type_single_chat == type) {
                SingleChatQueue item = new SingleChatQueue();
                item.setUid(uid);
                item.setToUid(thirdId);
                item.setMid(mid);
                item.setStatus(Status.unfinished);
                item.setTime(System.currentTimeMillis());
                res = singleChatQueueDao.insert(item);
            } else if(Type.type_many_chat == type) {
//                ChatGroup chatGroup = chatGroupDao.load(thirdId);
//                if(null != chatGroup) {
//                    List<Long> uids = JsonUtils.fromJson(chatGroup.getUids(),
//                            new TypeReference<ArrayList<Long>>() {});
//                    if(uids.contains(uid)) {
                        ManyChatQueue item = new ManyChatQueue();
                        item.setUid(uid);
                        item.setCgid(thirdId);
                        item.setMid(mid);
                        item.setStatus(Status.unfinished);
                        item.setTime(System.currentTimeMillis());
                        res = manyChatQueueDao.insert(item);
//                    } else {
//                        res = false;
//                    }
//                } else {
//                    res = false;
//                }
            }
        }
        return res;
    }

    @Override
    public boolean recall(long uid, String clientId, int type, long thirdId, long oldMid) throws Exception {
        if(Type.type_single_chat == type) {
            if(uid == thirdId) return false;
        }

        boolean res = false;
        Message oldMessage = messageDao.load(oldMid);
        if(null != oldMessage) {
            String recallExt = JsonUtils.toJson(new RecallExt(oldMid));
            MessageChat messageChat = new MessageChat(recallExt);
            String content = JsonUtils.toJson(messageChat);
            res = _insert(uid, clientId, type, thirdId, content);
        }
        return res;
    }

    @Override
    public boolean otherChat(long uid, String clientId, int type, long thirdId, MessageChat messageChat) throws Exception {
        if(Type.type_single_chat == type) {
            if(uid == thirdId) return false;
        }
        boolean res = false;
        String content = JsonUtils.toJson(messageChat);
        res = _insert(uid, clientId, type, thirdId, content);
        return res;
    }

    public boolean chat(long uid, String clientId, int type, long thirdId,
                        String word, String picture, String voice, String voiceDuration,
                        String video, String videoDuration, String ext) throws Exception {
		if(Type.type_single_chat == type) {
			if(uid == thirdId) return false;
		}
		
		boolean res = false;
		MessageChat messageChat = new MessageChat(word, picture, voice, voiceDuration, video, videoDuration, ext);
		String content = JsonUtils.toJson(messageChat);
		res = _insert(uid, clientId, type, thirdId, content);
		return res;
	}

	public boolean checkChat(long uid, String clientId) throws Exception {
		boolean res = false;
		UserClientInfo clientInfo = new UserClientInfo();
		clientInfo.setUid(uid);
		clientInfo.setClientId(clientId);
		clientInfo = userClientInfoDao.load(clientInfo);
		if(null != clientInfo) {
			int type = clientInfo.getType();
			long thirdId = clientInfo.getThirdId();
			long mid = clientInfo.getMid();
			if(Type.type_single_chat == type) {
				SingleChatQueue item = new SingleChatQueue();
				item.setUid(uid);
				item.setToUid(thirdId);
				item.setMid(mid);
				res = singleChatQueueDao.exist(item);
			} else if(Type.type_many_chat == type) {
				ManyChatQueue item = new ManyChatQueue();
				item.setUid(uid);
				item.setCgid(thirdId);
				item.setMid(mid);
				res = manyChatQueueDao.exist(item);
			}
		}
		return res;
	}

	public static void main(String[] args) {
	   Map<String, String> map = Maps.newHashMap();
	   map.put("1", "1");
	   String voice = JsonUtils.toJson(map);
	   System.out.println(voice);
	   Map<String, String> map2 = Maps.newHashMap();
	   map2.put("voice", voice);
	   String res = JsonUtils.toJson(map2);
	   System.out.println(res);
	}
}
