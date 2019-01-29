package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.NoticeAllQueueDao;
import com.quakoo.framework.ext.chat.model.NoticeAllQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.NoticeAllQueueService;


public class NoticeAllQueueServiceImpl implements NoticeAllQueueService {

	@Resource
	private NoticeAllQueueDao noticeAllQueueDao;

	public boolean unfinishedIsNull() throws Exception {
		return noticeAllQueueDao.list_null(Status.unfinished);
	}

	public List<NoticeAllQueue> unfinishedList(int size)
			throws Exception {
		return noticeAllQueueDao.all_list(Status.unfinished, size);
	}

	public List<NoticeAllQueue> finishedList(long maxTime, int size) throws Exception {
		return noticeAllQueueDao.list_time(Status.finished, maxTime, size);
	}

	public boolean updateStatus(NoticeAllQueue one, int newStatus)
			throws Exception {
		return noticeAllQueueDao.update(one, newStatus);
	}

}
