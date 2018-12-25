package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.ManyChatQueue;

public interface ManyChatQueueDao {
	
	public boolean insert(ManyChatQueue one) throws DataAccessException;
	
	public boolean exist(ManyChatQueue one) throws DataAccessException;
	
	public boolean delete(ManyChatQueue one) throws DataAccessException;
	
	public boolean update(ManyChatQueue one, int newStatus) throws DataAccessException;
	
	public List<ManyChatQueue> all_list(String table_name, int status,
			int size) throws DataAccessException;
	
	public List<ManyChatQueue> list_time(String table_name, int status, 
			long maxTime, int size) throws DataAccessException;
	
	public boolean list_null(String table_name, int status);
	
}
