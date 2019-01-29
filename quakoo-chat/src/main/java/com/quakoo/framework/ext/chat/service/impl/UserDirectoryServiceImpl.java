package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.dao.UserDirectoryDao;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.sun.org.apache.xpath.internal.operations.Bool;


/**
 * 用户消息目录处理类
 * class_name: UserDirectoryServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:21
 **/
public class UserDirectoryServiceImpl implements UserDirectoryService {

	@Resource
	private UserDirectoryDao userDirectoryDao;

	/**
     * 批量插入消息目录
	 * method_name: batchInsert
	 * params: [directories]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:21
	 **/
	public void batchInsert(List<UserDirectory> directories) throws Exception {
		userDirectoryDao.insert(directories);
	}

	/**
     * 过滤消息目录
	 * method_name: filterExists
	 * params: [directories]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.UserDirectory>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:21
	 **/
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
