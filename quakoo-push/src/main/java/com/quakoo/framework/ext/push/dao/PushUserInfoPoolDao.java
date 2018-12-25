package com.quakoo.framework.ext.push.dao;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

import com.quakoo.framework.ext.push.model.PushUserInfoPool;

public interface PushUserInfoPoolDao {

	public boolean insert(PushUserInfoPool one) throws DataAccessException;

	public boolean delete(PushUserInfoPool one) throws DataAccessException;

	public boolean clear(long uid) throws DataAccessException;

	public List<PushUserInfoPool> getPushUserInfos(long uid) throws DataAccessException;

	public Map<Long, List<PushUserInfoPool>> getPushUserInfos(List<Long> uids)
			throws DataAccessException;

}
