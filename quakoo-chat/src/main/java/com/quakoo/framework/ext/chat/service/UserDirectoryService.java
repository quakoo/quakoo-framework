package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.UserDirectory;

public interface UserDirectoryService {
	
	public void batchInsert(List<UserDirectory> directories) throws Exception;
	
}