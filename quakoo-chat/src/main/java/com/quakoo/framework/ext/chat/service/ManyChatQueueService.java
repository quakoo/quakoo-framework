package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.ManyChatQueue;

public interface ManyChatQueueService {
	
	public boolean unfinishedIsNull(String tableName) throws Exception;
	
	public List<ManyChatQueue> unfinishedList(String tableName, int size) throws Exception;
	
	public List<ManyChatQueue> finishedList(String tableName, long maxTime, int size) throws Exception;
	
	public boolean updateStatus(ManyChatQueue one, int newStatus) throws Exception;
	
}
