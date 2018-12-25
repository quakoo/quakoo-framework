package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.SingleChatQueue;

public interface SingleChatQueueDao {
	
	public boolean insert(SingleChatQueue one) throws DataAccessException;
	
	public boolean exist(SingleChatQueue one) throws DataAccessException;
	
	public boolean delete(SingleChatQueue one) throws DataAccessException;
	
	public boolean update(SingleChatQueue one, int newStatus) throws DataAccessException;
	
	public List<SingleChatQueue> all_list(String table_name, int status,
			int size) throws DataAccessException;
	
	public List<SingleChatQueue> list_time(String table_name, int status, 
			long maxTime, int size) throws DataAccessException;
	
	public boolean list_null(String table_name, int status);
	
}
