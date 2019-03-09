package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.NoticeRangeQueue;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;

public interface NoticeRangeQueueService {
	
//    public boolean unfinishedIsNull() throws Exception;
    
//	public List<NoticeRangeQueue> unfinishedList(int size) throws Exception;
	
//	public List<NoticeRangeQueue> finishedList(long maxTime, int size) throws Exception;
	
//	public boolean updateStatus(NoticeRangeQueue one, int newStatus) throws Exception;


    public List<NoticeRangeQueue> list(int size) throws Exception;

    public void delete(NoticeRangeQueue one) throws Exception;

    public void delete(List<NoticeRangeQueue> queues) throws Exception;

}
