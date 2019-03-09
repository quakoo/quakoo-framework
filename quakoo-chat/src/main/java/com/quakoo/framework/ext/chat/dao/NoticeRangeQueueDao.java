package com.quakoo.framework.ext.chat.dao;


import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.NoticeRangeQueue;

import java.util.List;

public interface NoticeRangeQueueDao {
	
//	public boolean insert(NoticeRangeQueue one) throws DataAccessException;
//
//	public boolean update(NoticeRangeQueue one, int newStatus) throws DataAccessException;
//
//	public List<NoticeRangeQueue> all_list(int status, int size) throws DataAccessException;
//
//	public List<NoticeRangeQueue> list_time(int status, long maxTime, int size) throws DataAccessException;
//
//	public boolean list_null(int status);


    public boolean insert(NoticeRangeQueue one) throws DataAccessException;

    public boolean delete(NoticeRangeQueue one) throws DataAccessException;

    public void delete(List<NoticeRangeQueue> queues) throws DataAccessException;

    public List<NoticeRangeQueue> list(int size) throws DataAccessException;


}
