package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.UserPrompt;

public interface UserPromptDao {
	
	public boolean insert(UserPrompt userPrompt) throws DataAccessException;
	
	public int insert(List<UserPrompt> userPrompts) throws DataAccessException;
	
	public List<UserPrompt> new_data(long uid, double index) throws Exception;
	
}
