package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.UserInfo;

public interface UserInfoDao {
	
	public double create_login_time(long uid) throws Exception;
	
//	public UserInfo insert(UserInfo userInfo) throws DataAccessException;
	
	public UserInfo load(long uid) throws DataAccessException;
	
	public List<UserInfo> loads(List<Long> uids) throws DataAccessException;
	
//	public boolean update(UserInfo userInfo) throws DataAccessException;
	
	public boolean update_prompt_index(long uid, double promptIndex) throws DataAccessException;
	
	public List<UserInfo> list(String table_name, double loginTime, int size)
			throws DataAccessException;

	public UserInfo sync(long uid, double lastIndex, double loginTime, UserInfo userInfo) throws Exception;
	
}
