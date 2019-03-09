package com.quakoo.framework.ext.chat.dao;

import com.quakoo.framework.ext.chat.model.UserInfoQueue;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface UserInfoQueueDao {

    public void insert(List<UserInfoQueue> queues) throws DataAccessException;

    public void delete(List<Long> uids) throws DataAccessException;

    public List<Long> list(String queue_name, int size) throws DataAccessException;

}
