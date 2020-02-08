package com.quakoo.space.interfaces;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;


public interface HDao<T> extends Serializable {

	public T load(Object id) throws DataAccessException;

	public List<T> load(List objs) throws Exception;

	public T insert(T model) throws DataAccessException;

	public boolean update(T model) throws DataAccessException;

	public boolean delete(Object id) throws DataAccessException;

	public Map<String, List<String>> getCacheMap();
	
	public T increment(T model,String filedName,int incrementValue) throws Exception;



	/**
	 * 修改,带ZK锁
	 * 避免修改并发问题
	 *
	 * @param id 需要加载的主键，参考load方法
	 * @param filedKV 对应要修改的属性-值
	 * @return
	 * @throws DataAccessException
	 */
	public boolean zkLockAndUpdate(Object id,Map<String,Object> filedKV) throws DataAccessException;

	/**
	 * 修改,带ZK锁，
	 * 避免修改并发问题
	 * @return
	 * @throws DataAccessException
	 */
	public T zkLockAndIncrement(Object id, String filedName, int incrementValue) throws Exception;


}
