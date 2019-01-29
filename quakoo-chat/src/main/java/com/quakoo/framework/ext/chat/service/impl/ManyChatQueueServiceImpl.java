package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.ManyChatQueueDao;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.ManyChatQueueService;

/**
 * 群聊消息队列处理类
 * class_name: ManyChatQueueServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:19
 **/
public class ManyChatQueueServiceImpl implements ManyChatQueueService {

	@Resource
	private ManyChatQueueDao manyChatQueueDao;

	@Override
	public boolean unfinishedIsNull(String tableName) throws Exception {
		return manyChatQueueDao.list_null(tableName, Status.unfinished);
	}

    @Override
	public List<ManyChatQueue> unfinishedList(String tableName, int size)
			throws Exception {
		return manyChatQueueDao.all_list(tableName, Status.unfinished, size);
	}

    @Override
	public boolean updateStatus(ManyChatQueue one, int newStatus)
			throws Exception {
		return manyChatQueueDao.update(one, newStatus);
	}

    @Override
    public void updateStatus(List<ManyChatQueue> list, int newStatus) throws Exception {
        manyChatQueueDao.update(list, newStatus);
    }

    //    @Override
//	public List<ManyChatQueue> finishedList(String tableName, long maxTime,
//			int size) throws Exception {
//		return manyChatQueueDao.list_time(tableName, Status.finished, maxTime, size);
//	}

}
