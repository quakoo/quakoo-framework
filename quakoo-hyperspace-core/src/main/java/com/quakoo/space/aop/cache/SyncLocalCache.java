package com.quakoo.space.aop.cache;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.quakoo.baseFramework.localCache.LongKeyLocalCache;
import com.quakoo.space.AbstractCacheBaseDao;

public class SyncLocalCache {

	String cacheKey;
	String isNullCacheKey;
	AbstractCacheBaseDao<?> dao;
	Object[] newArgs;
	Method relationMethod;
	LongKeyLocalCache isListLoaclCache;
	LongKeyLocalCache isNullLoaclCache;
	Object arg;
	@Override
	public String toString() {
		return "SyncLocalCache [cacheKey=" + cacheKey + ", isNullCacheKey="
				+ isNullCacheKey + ", dao=" + dao + ", newArgs="
				+ Arrays.toString(newArgs) + ", relationMethod="
				+ relationMethod + ", isListLoaclCache=" + isListLoaclCache
				+ ", isNullLoaclCache=" + isNullLoaclCache + ", arg=" + arg
				+ "]";
	}
	public String getCacheKey() {
		return cacheKey;
	}
	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}
	public String getIsNullCacheKey() {
		return isNullCacheKey;
	}
	public void setIsNullCacheKey(String isNullCacheKey) {
		this.isNullCacheKey = isNullCacheKey;
	}
	public AbstractCacheBaseDao<?> getDao() {
		return dao;
	}
	public void setDao(AbstractCacheBaseDao<?> dao) {
		this.dao = dao;
	}
	public Object[] getNewArgs() {
		return newArgs;
	}
	public void setNewArgs(Object[] newArgs) {
		this.newArgs = newArgs;
	}
	public Method getRelationMethod() {
		return relationMethod;
	}
	public void setRelationMethod(Method relationMethod) {
		this.relationMethod = relationMethod;
	}
	public LongKeyLocalCache getIsListLoaclCache() {
		return isListLoaclCache;
	}
	public void setIsListLoaclCache(LongKeyLocalCache isListLoaclCache) {
		this.isListLoaclCache = isListLoaclCache;
	}
	public LongKeyLocalCache getIsNullLoaclCache() {
		return isNullLoaclCache;
	}
	public void setIsNullLoaclCache(LongKeyLocalCache isNullLoaclCache) {
		this.isNullLoaclCache = isNullLoaclCache;
	}
	public Object getArg() {
		return arg;
	}
	public void setArg(Object arg) {
		this.arg = arg;
	}
	public SyncLocalCache(String cacheKey, String isNullCacheKey,
			AbstractCacheBaseDao<?> dao, Object[] newArgs,
			Method relationMethod, LongKeyLocalCache isListLoaclCache,
			LongKeyLocalCache isNullLoaclCache, Object arg) {
		super();
		this.cacheKey = cacheKey;
		this.isNullCacheKey = isNullCacheKey;
		this.dao = dao;
		this.newArgs = newArgs;
		this.relationMethod = relationMethod;
		this.isListLoaclCache = isListLoaclCache;
		this.isNullLoaclCache = isNullLoaclCache;
		this.arg = arg;
	}

	
	
}
