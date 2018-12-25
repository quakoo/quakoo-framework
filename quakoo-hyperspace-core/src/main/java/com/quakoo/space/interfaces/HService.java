package com.quakoo.space.interfaces;

import java.io.Serializable;
import java.util.List;

import org.springframework.dao.DataAccessException;

public interface HService<T> extends Serializable {

	/**
	 *
	 */
	public static final int default_max_list_size=10000;

	public T load(Object id) throws Exception;

	public List<T> load(List objs) throws Exception;

	public T insert(T model) throws Exception;

	public boolean update(T model) throws Exception;

	public boolean delete(Object id) throws Exception;

	public List<String> getCacheByType(String type);

}
