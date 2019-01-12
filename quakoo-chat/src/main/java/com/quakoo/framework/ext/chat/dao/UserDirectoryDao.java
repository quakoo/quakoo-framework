package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.UserDirectory;

public interface UserDirectoryDao {
	
//    public void insert(UserDirectory messageDirectory) throws DataAccessException;

    public List<UserDirectory> load(List<UserDirectory> ids) throws DataAccessException;

	public void insert(List<UserDirectory> messageDirectories)
			throws DataAccessException;
	
	public List<UserDirectory> list_all(long uid) throws DataAccessException;
	
}