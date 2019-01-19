package com.quakoo.framework.ext.push.dao;

import com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface PushMsgHandleAllQueueDao {

    public boolean insert(PushMsgHandleAllQueue one) throws DataAccessException;

    public List<PushMsgHandleAllQueue> getList(long minId, int size) throws DataAccessException;

    public PushMsgHandleAllQueue load(long id) throws DataAccessException;

}
