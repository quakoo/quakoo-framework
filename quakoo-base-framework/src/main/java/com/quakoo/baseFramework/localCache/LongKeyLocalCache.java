package com.quakoo.baseFramework.localCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LongKeyLocalCache extends LocalCache {

	public LongKeyLocalCache(String name) {
		super();
		this.name = name;
	}

	private ConcurrentHashMap<Long, LocalCacheModel> store = new ConcurrentHashMap<Long, LocalCacheModel>();

	/**
	 * 永久
	 * 
	 * @param key
	 * @param value
	 */
	public void put(Long key, Object value) {
		this.put(key, value, 0);
	}

	public void put(Long key, Object value, long overtime) {
		if (overtime != 0) {
			overtime += System.currentTimeMillis();
		}
		LocalCacheModel model = new LocalCacheModel(value, overtime);
		this.store.put(key, model);
	}

	public void putAll(Map<Long, Object> map) {
		this.putAll(map, 0);
	}

	public void putAll(Map<Long, Object> map, long overtime) {
		if (overtime != 0) {
			overtime += System.currentTimeMillis();
		}
		Map<Long, LocalCacheModel> temp = new HashMap<Long, LocalCacheModel>();
		for (Long key : map.keySet()) {
			Object value = map.get(key);
			LocalCacheModel model = new LocalCacheModel(value, overtime);
			temp.put(key, model);
		}
		this.store.putAll(temp);
	}

	public Object get(Long key) {
		LocalCacheModel model = this.store.get(key);
		if (null == model)
			return null;
		if (model.getOvertime() == 0) {
			return model.getValue();
		} else {
			long currentTime = System.currentTimeMillis();
			if (currentTime >= model.getOvertime()){
				store.remove(key);
				return null;
			}else
				return model.getValue();
		}
	}
	
	public Set<Long> getKesSet(){
		return store.keySet();
	}
	
}
