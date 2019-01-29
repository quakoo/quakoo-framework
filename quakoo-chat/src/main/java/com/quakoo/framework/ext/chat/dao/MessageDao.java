package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.Message;

public interface MessageDao {
	
    public Message insert(Message message) throws DataAccessException;

    public boolean updateContent(long id, String content) throws DataAccessException;
    
	public Message load(long id) throws DataAccessException;
	
	public List<Message> load(List<Long> ids) throws DataAccessException;

    public Message tempLoad(long authorId, String clientId) throws DataAccessException;



    public List<Long> getMessageIds(int size) throws DataAccessException;

    public List<Message> insert(List<Message> messages) throws DataAccessException;

}
