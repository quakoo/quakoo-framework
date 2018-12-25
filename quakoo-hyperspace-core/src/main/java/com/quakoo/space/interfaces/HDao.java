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
}
