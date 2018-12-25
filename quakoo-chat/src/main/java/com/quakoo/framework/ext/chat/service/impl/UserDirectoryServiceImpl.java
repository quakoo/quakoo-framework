package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.UserDirectoryDao;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;


public class UserDirectoryServiceImpl implements UserDirectoryService {

	@Resource
	private UserDirectoryDao userDirectoryDao;
	
	public void batchInsert(List<UserDirectory> directories) throws Exception {
		userDirectoryDao.insert(directories);
	}

}
