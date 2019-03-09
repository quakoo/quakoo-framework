package com.quakoo.framework.ext.chat.dao;


import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.NoticeAllQueue;

import java.util.List;

public interface NoticeAllQueueDao {
	
//	public boolean insert(NoticeAllQueue one) throws DataAccessException;
//
//	public boolean update(NoticeAllQueue one, int newStatus) throws DataAccessException;
//
//	public List<NoticeAllQueue> all_list(int status, int size) throws DataAccessException;
//
//	public List<NoticeAllQueue> list_time(int status, long maxTime, int size) throws DataAccessException;
//
//	public boolean list_null(int status);

    public boolean insert(NoticeAllQueue one) throws DataAccessException;

    public boolean delete(NoticeAllQueue one) throws DataAccessException;

    public List<NoticeAllQueue> list(int size) throws DataAccessException;

}
