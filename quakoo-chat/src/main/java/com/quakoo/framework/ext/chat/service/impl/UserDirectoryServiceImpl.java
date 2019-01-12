package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.dao.UserDirectoryDao;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;


public class UserDirectoryServiceImpl implements UserDirectoryService {

	@Resource
	private UserDirectoryDao userDirectoryDao;
	
	public void batchInsert(List<UserDirectory> directories) throws Exception {
		userDirectoryDao.insert(directories);
	}

    @Override
    public List<UserDirectory> filterExists(List<UserDirectory> directories) throws Exception {
        List<UserDirectory> list = userDirectoryDao.load(directories);
        Map<String, UserDirectory> map = Maps.newHashMap();
        for(UserDirectory one : list) {
            if(null != one) map.put(String.format("%d_%d_%d", one.getUid(), one.getType(), one.getThirdId()), one);
        }
        List<UserDirectory> res = Lists.newArrayList();
        for(UserDirectory directory : directories) {
            String key = String.format("%d_%d_%d", directory.getUid(), directory.getType(), directory.getThirdId());
            if(null == map.get(key)) {
                res.add(directory);
            }
        }
        return res;
    }

}
