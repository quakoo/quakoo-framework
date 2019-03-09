package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.NoticeRangeQueueDao;
import com.quakoo.framework.ext.chat.model.NoticeRangeQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.service.NoticeRangeQueueService;


public class NoticeRangeQueueServiceImpl implements NoticeRangeQueueService {

	@Resource
	private NoticeRangeQueueDao noticeRangeQueueDao;

//	public boolean unfinishedIsNull() throws Exception {
//		return noticeRangeQueueDao.list_null(Status.unfinished);
//	}
//
//	public List<NoticeRangeQueue> unfinishedList(int size) throws Exception {
//		return noticeRangeQueueDao.all_list(Status.unfinished, size);
//	}
//
//	public List<NoticeRangeQueue> finishedList(long maxTime, int size)
//			throws Exception {
//		return noticeRangeQueueDao.list_time(Status.finished, maxTime, size);
//	}
//
//	public boolean updateStatus(NoticeRangeQueue one, int newStatus)
//			throws Exception {
//		return noticeRangeQueueDao.update(one, newStatus);
//	}


    @Override
    public List<NoticeRangeQueue> list(int size) throws Exception {
        return noticeRangeQueueDao.list(size);
    }

    @Override
    public void delete(NoticeRangeQueue one) throws Exception {
        noticeRangeQueueDao.delete(one);
    }

    @Override
    public void delete(List<NoticeRangeQueue> queues) throws Exception {
        noticeRangeQueueDao.delete(queues);
    }
}
