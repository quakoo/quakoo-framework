package com.quakoo.framework.ext.push.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.push.model.PushUserQueue;

public interface PushUserQueueDao {

    public void insert(List<Long> uids) throws DataAccessException;

    public void delete(List<Long> uids) throws DataAccessException;

//	public void insert(PushUserQueue one) throws DataAccessException;

    public List<PushUserQueue> getList(String table_name, long index, int size) throws DataAccessException;
}
