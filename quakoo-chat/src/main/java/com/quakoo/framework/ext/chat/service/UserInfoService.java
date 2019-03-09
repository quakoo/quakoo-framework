package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.UserInfo;
import org.springframework.dao.DataAccessException;

public interface UserInfoService {
	
	public UserInfo syncUserInfo(long uid, double lastIndex, UserInfo userInfo) throws Exception;
	
	public boolean updatePromptIndex(long uid, double promptIndex) throws Exception;
	
	public List<UserInfo> list(String tableName, double loginTime, int size) throws Exception;
	
	public List<UserInfo> load(List<Long> uids) throws Exception;

    public void replace(List<UserInfo> userInfos) throws Exception; //更新数据库

	public UserInfo load(long uid) throws Exception;

	public List<UserInfo> loadCache(List<Long> uids) throws Exception;
	
}
