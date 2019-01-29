package com.quakoo.framework.ext.chat.service;

import java.util.List;
import java.util.Map;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.back.StreamBack;

public interface UserStreamService {
	
	 public void init(long uid) throws Exception;
	 
	 public List<UserStream> newStream(long uid, double lastIndex) throws Exception;
	 
	 public Map<Long, List<UserStream>> newStream(Map<Long, Double> lastIndexMap) throws Exception;
	 
	 public boolean delete(long uid, int type, long thirdId, long mid) throws Exception;
	 
	 public Pager getPager(long uid, int type, long thirdId, Pager pager) throws Exception;
	 
	 public int batchInsert(List<UserStream> streams) throws Exception;
	 
	 public List<StreamBack> transformBack(List<UserStream> list) throws Exception;


	 public List<UserStream> getDirectoryStream(long uid, double lastIndex) throws Exception;
	 
}