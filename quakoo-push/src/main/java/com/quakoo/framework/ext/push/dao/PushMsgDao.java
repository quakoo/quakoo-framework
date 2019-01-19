package com.quakoo.framework.ext.push.dao;

import com.quakoo.framework.ext.push.model.PushMsg;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface PushMsgDao {

    public long createId();

    public void insert(List<PushMsg> list) throws DataAccessException;

    public void update(List<PushMsg> list) throws DataAccessException;

}
