package com.quakoo.framework.ext.chat.dao;

import com.quakoo.framework.ext.chat.model.UserChatGroupPool;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface UserChatGroupPoolDao {

    public int insert(List<UserChatGroupPool> pools) throws DataAccessException;

    public int delete(List<UserChatGroupPool> pools) throws DataAccessException;

    public int update(UserChatGroupPool pool) throws DataAccessException;

    public UserChatGroupPool load(UserChatGroupPool pool) throws DataAccessException;

    public List<UserChatGroupPool> load(List<UserChatGroupPool> pools) throws DataAccessException;

    public List<UserChatGroupPool> list(long uid) throws Exception;

}
