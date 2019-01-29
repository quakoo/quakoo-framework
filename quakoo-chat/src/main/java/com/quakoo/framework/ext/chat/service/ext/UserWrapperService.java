package com.quakoo.framework.ext.chat.service.ext;

import java.util.List;

import com.quakoo.framework.ext.chat.model.back.UserBack;

/**
 * 用户包装类
 * class_name: UserWrapperService
 * package: com.quakoo.framework.ext.chat.service.ext
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:13
 **/
public interface UserWrapperService {

    /**
     * 封装用户信息
     * method_name: getUsers
     * params: [uids]
     * return: java.util.List<com.quakoo.framework.ext.chat.model.back.UserBack>
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 17:13
     **/
	public List<UserBack> getUsers(List<Long> uids) throws Exception;

	/**
     * 封装用户备注信息
	 * method_name: getRemarkUsers
	 * params: [uid, uids]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.back.UserBack>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:14
	 **/
	public List<UserBack> getRemarkUsers(long uid, List<Long> uids) throws Exception;

}
