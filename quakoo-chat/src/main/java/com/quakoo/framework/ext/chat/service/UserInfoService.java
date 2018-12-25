package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.UserInfo;

public interface UserInfoService {
	
	public UserInfo syncUserInfo(long uid, double lastIndex, UserInfo userInfo) throws Exception;
	
	public boolean updatePromptIndex(long uid, double promptIndex) throws Exception;
	
	public List<UserInfo> list(String tableName, double loginTime, int size) throws Exception;
	
	public List<UserInfo> load(List<Long> uids) throws Exception;

	public UserInfo load(long uid) throws Exception;
	
}
