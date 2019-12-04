package com.quakoo.framework.ext.chat.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.param.UserOneStreamParam;
import com.quakoo.framework.ext.chat.model.param.UserStreamParam;

public interface UserStreamDao {
	
	public void init(long uid, boolean lockSign) throws Exception;
	
	public void init_sub(long uid, int type, long thirdId, boolean lockSign) throws Exception;
	
	public int insert_cold_data(List<UserStream> streams) throws DataAccessException; //插入冷数据

    public int insert_hot_data(List<UserStream> streams) throws DataAccessException; //插入热数据
	
	public void create_sort(List<UserStream> streams) throws Exception;
	
	public UserStream load(UserStream one) throws DataAccessException;
	
	public boolean delete(UserStream one) throws DataAccessException;
	
	public List<UserStream> page_list(long uid, long type, long thirdId, 
			double cursor, int size) throws DataAccessException; 
	
	public void new_cold_data(List<UserStreamParam> list) throws Exception; //获取一批用户的冷数据

	public void new_hot_data(List<UserStreamParam> list) throws Exception; //获取一批用户的热数据

	public void one_new_cold_data(List<UserOneStreamParam> list) throws Exception; //获取一批用户的冷数据(子集)


    public void clear_hot_data_by_sort(long uid, double sort) throws Exception;
	
}
