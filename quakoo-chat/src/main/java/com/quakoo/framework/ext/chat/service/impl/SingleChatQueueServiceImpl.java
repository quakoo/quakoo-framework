package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.SingleChatQueueDao;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.SingleChatQueueService;


public class SingleChatQueueServiceImpl implements SingleChatQueueService {

	@Resource
	private SingleChatQueueDao singleChatQueueDao;
	
	public boolean unfinishedIsNull(String tableName) throws Exception {
		return singleChatQueueDao.list_null(tableName, Status.unfinished);
	}

	public List<SingleChatQueue> unfinishedList(String tableName, int size)
			throws Exception {
		return singleChatQueueDao.all_list(tableName, Status.unfinished, size);
	}

	public boolean updateStatus(SingleChatQueue one, int newStatus)
			throws Exception {
		return singleChatQueueDao.update(one, newStatus);
	}

	public List<SingleChatQueue> finishedList(String tableName, long maxTime,
			int size) throws Exception {
		return singleChatQueueDao.list_time(tableName, Status.finished, maxTime, size);
	}

}
