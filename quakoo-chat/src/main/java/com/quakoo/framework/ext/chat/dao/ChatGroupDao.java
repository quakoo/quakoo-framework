package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.ChatGroup;

public interface ChatGroupDao{
	
	public ChatGroup insert(ChatGroup chatGroup) throws DataAccessException;
	
	public ChatGroup load(long id) throws DataAccessException;
	
	public List<ChatGroup> load(List<Long> ids) throws DataAccessException;
	
	public boolean update(ChatGroup chatGroup) throws DataAccessException;
	
}