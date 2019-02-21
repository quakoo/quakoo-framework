package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.SingleChatQueueDao;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.SingleChatQueueService;

/**
 * 单聊消息队列处理类
 * class_name: SingleChatQueueServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:20
 **/
public class SingleChatQueueServiceImpl implements SingleChatQueueService {

    @Resource
    private SingleChatQueueDao singleChatQueueDao;

//	@Override
//	public boolean unfinishedIsNull(String tableName) throws Exception {
//		return singleChatQueueDao.list_null(tableName, Status.unfinished);
//	}

//    @Override
//	public List<SingleChatQueue> unfinishedList(String tableName, int size)
//			throws Exception {
//		return singleChatQueueDao.all_list(tableName, Status.unfinished, size);
//	}

//    @Override
//	public boolean updateStatus(SingleChatQueue one, int newStatus)
//			throws Exception {
//		return singleChatQueueDao.update(one, newStatus);
//	}

//    @Override
//    public void updateStatus(List<SingleChatQueue> list, int newStatus) throws Exception {
//        singleChatQueueDao.update(list, newStatus);
//    }

    //    @Override
//	public List<SingleChatQueue> finishedList(String tableName, long maxTime,
//			int size) throws Exception {
//		return singleChatQueueDao.list_time(tableName, Status.finished, maxTime, size);
//	}

    @Override
    public List<SingleChatQueue> list(String queueName, int size) throws Exception {
        return singleChatQueueDao.list(queueName, size);
    }

    @Override
    public void delete(List<SingleChatQueue> queues) throws Exception {
        singleChatQueueDao.delete(queues);
    }

}
