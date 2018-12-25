package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.param.UserOneStreamParam;
import com.quakoo.framework.ext.chat.model.param.UserStreamParam;

public interface UserStreamDao {
	
	public void init(long uid, boolean lockSign) throws Exception;
	
	public void init_sub(long uid, int type, long thirdId, boolean lockSign) throws Exception;
	
	public int insert(List<UserStream> streams) throws DataAccessException;
	
	public void create_sort(List<UserStream> streams) throws Exception;
	
	public UserStream load(UserStream one) throws DataAccessException;
	
	public boolean delete(UserStream one) throws DataAccessException;
	
	public List<UserStream> page_list(long uid, long type, long thirdId, 
			double cursor, int size) throws DataAccessException; 
	
	public void new_data(List<UserStreamParam> list) throws Exception;
	
	public void one_new_data(List<UserOneStreamParam> list) throws Exception;
	
}
