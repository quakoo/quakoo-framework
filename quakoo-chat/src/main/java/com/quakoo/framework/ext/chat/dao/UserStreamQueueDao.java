package com.quakoo.framework.ext.chat.dao;

import com.quakoo.framework.ext.chat.model.UserStreamQueue;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface UserStreamQueueDao {

    public void insert(List<UserStreamQueue> queues) throws DataAccessException;

    public void delete(List<UserStreamQueue> queues) throws DataAccessException;

    public List<UserStreamQueue> list(String queue_name, int size) throws DataAccessException;

}
