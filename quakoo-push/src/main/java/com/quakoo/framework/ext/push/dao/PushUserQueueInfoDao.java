package com.quakoo.framework.ext.push.dao;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.push.model.PushUserQueueInfo;

import java.util.List;

public interface PushUserQueueInfoDao {

	public void insert(PushUserQueueInfo one) throws DataAccessException;
	
	public PushUserQueueInfo load(String tableName) throws DataAccessException;
	
	public boolean update(PushUserQueueInfo one) throws DataAccessException;

}
