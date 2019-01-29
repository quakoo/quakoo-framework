package com.quakoo.framework.ext.chat.service;

import java.util.List;
import java.util.Map;

import com.quakoo.framework.ext.chat.model.UserDirectory;

public interface UserDirectoryService {

    public List<UserDirectory> filterExists(List<UserDirectory> directories) throws Exception;

	public void batchInsert(List<UserDirectory> directories) throws Exception;
	
}