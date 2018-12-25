package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.NoticeAllQueue;

public interface NoticeAllQueueService {
	
    public boolean unfinishedIsNull() throws Exception;
    
	public List<NoticeAllQueue> unfinishedList(int size) throws Exception;
	
	public List<NoticeAllQueue> finishedList(long maxTime, int size) throws Exception;
	
	public boolean updateStatus(NoticeAllQueue one, int newStatus) throws Exception;
	
}
