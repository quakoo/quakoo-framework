package com.quakoo.space.aop.cache;

import com.quakoo.space.AbstractCacheBaseDao;

public class MethodDaoInfo {
	
	AbstractCacheBaseDao<?> dao;
	Object[] args;
	int index;
	
	public AbstractCacheBaseDao<?> getDao() {
		return dao;
	}
	public void setDao(AbstractCacheBaseDao<?> dao) {
		this.dao = dao;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public MethodDaoInfo(AbstractCacheBaseDao<?> dao, Object[] args, int index) {
		super();
		this.dao = dao;
		this.args = args;
		this.index = index;
	}
	
	
	
	
	
	
}
