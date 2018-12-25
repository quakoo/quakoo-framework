package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.ManyChatQueueDao;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.ManyChatQueueService;


public class ManyChatQueueServiceImpl implements ManyChatQueueService {

	@Resource
	private ManyChatQueueDao manyChatQueueDao;

	public boolean unfinishedIsNull(String tableName) throws Exception {
		return manyChatQueueDao.list_null(tableName, Status.unfinished);
	}

	public List<ManyChatQueue> unfinishedList(String tableName, int size)
			throws Exception {
		return manyChatQueueDao.all_list(tableName, Status.unfinished, size);
	}

	public boolean updateStatus(ManyChatQueue one, int newStatus)
			throws Exception {
		return manyChatQueueDao.update(one, newStatus);
	}

	public List<ManyChatQueue> finishedList(String tableName, long maxTime,
			int size) throws Exception {
		return manyChatQueueDao.list_time(tableName, Status.finished, maxTime, size);
	}

}
