package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.NoticeRangeQueue;

public interface NoticeRangeQueueDao {
	
	public boolean insert(NoticeRangeQueue one) throws DataAccessException;
	
	public boolean update(NoticeRangeQueue one, int newStatus) throws DataAccessException;
	
	public List<NoticeRangeQueue> all_list(int status, int size) throws DataAccessException;
	
	public List<NoticeRangeQueue> list_time(int status, long maxTime, int size) throws DataAccessException;
	
	public boolean list_null(int status);
	
}
