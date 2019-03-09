package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.dao.NoticeAllQueueDao;
import com.quakoo.framework.ext.chat.dao.NoticeRangeQueueDao;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.MessageNotice;
import com.quakoo.framework.ext.chat.model.NoticeAllQueue;
import com.quakoo.framework.ext.chat.model.NoticeRangeQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.service.NoticeService;

public class NoticeServiceImpl implements NoticeService {

	@Resource
	private MessageDao messageDao;
	
	@Resource
	private NoticeAllQueueDao noticeAllQueueDao;
	
	@Resource
	private NoticeRangeQueueDao noticeRangeQueueDao;
	
	public boolean noticeAll(long authorId, String title, String desc,
			String cover, String redirect) throws Exception {
		MessageNotice messageNotice = new MessageNotice(title, desc, cover, redirect);
		String content = JsonUtils.toJson(messageNotice);
		Message message = new Message();
		message.setAuthorId(authorId);
        message.setClientId(String.valueOf(System.currentTimeMillis()));
		message.setType(Type.type_notice);
		message.setContent(content);
		message = messageDao.insert(message);
		long mid = message.getId();
		NoticeAllQueue item = new NoticeAllQueue();
		item.setAuthorId(authorId);
		item.setMid(mid);
		item.setStatus(Status.unfinished);
		item.setTime(System.currentTimeMillis());
		return noticeAllQueueDao.insert(item);
	}

	public boolean noticeRange(long authorId, List<Long> uids, String title,
			String desc, String cover, String redirect) throws Exception {
		MessageNotice messageNotice = new MessageNotice(title, desc, cover, redirect);
		String content = JsonUtils.toJson(messageNotice);
		Message message = new Message();
		message.setAuthorId(authorId);
        message.setClientId(String.valueOf(System.currentTimeMillis()));
		message.setType(Type.type_notice);
		message.setContent(content);
		message = messageDao.insert(message);
		long mid = message.getId();
		NoticeRangeQueue item = new NoticeRangeQueue();
		item.setAuthorId(authorId);
		item.setMid(mid);
		item.setStatus(Status.unfinished);
		String uidStr = JsonUtils.toJson(uids);
		item.setUids(uidStr);
		item.setTime(System.currentTimeMillis());
		return noticeRangeQueueDao.insert(item);
	}

}
