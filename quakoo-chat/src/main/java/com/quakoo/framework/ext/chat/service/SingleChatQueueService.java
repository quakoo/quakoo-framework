package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;

public interface SingleChatQueueService {

//    public boolean unfinishedIsNull(String tableName) throws Exception;

//	public List<SingleChatQueue> unfinishedList(String tableName, int size) throws Exception;

//	public List<SingleChatQueue> finishedList(String tableName, long maxTime, int size) throws Exception;

//	public boolean updateStatus(SingleChatQueue one, int newStatus) throws Exception;

//    public void updateStatus(List<SingleChatQueue> list, int newStatus) throws Exception;

    public List<SingleChatQueue> list(String queueName, int size) throws Exception;

    public void delete(List<SingleChatQueue> queues) throws Exception;

}
