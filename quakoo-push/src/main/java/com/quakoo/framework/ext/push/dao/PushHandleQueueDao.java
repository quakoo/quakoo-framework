package com.quakoo.framework.ext.push.dao;


import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.push.model.PushHandleQueue;


public interface PushHandleQueueDao {
	
	public boolean insert(PushHandleQueue one) throws DataAccessException;
	
	public boolean delete(PushHandleQueue one) throws DataAccessException;
	
	public List<PushHandleQueue> list(String table_name, int size) throws DataAccessException;

}
