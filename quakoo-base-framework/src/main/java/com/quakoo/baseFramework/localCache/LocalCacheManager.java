package com.quakoo.baseFramework.localCache;

import java.util.HashMap;
import java.util.Map;

public class LocalCacheManager {

	private Object lock = new Object();

	private static final Map<String, LocalCache> store = new HashMap<String, LocalCache>();

	public LocalCacheManager() {
	}

	public void init(LocalCache localCache) {
		synchronized (lock) {
			String name = localCache.getName();
			if (store.keySet().contains(name)) {
				throw new IllegalStateException("this cache is exists");
			}
			store.put(name, localCache);
		}
	}

	public LocalCache pollCache(String name) {
		if (!store.keySet().contains(name))
			throw new IllegalStateException("this cache is not exists");
		return store.get(name);
	}

	public static void main(String[] args) throws InterruptedException {
		LocalCacheManager manager = new LocalCacheManager();
		manager.init(new LongKeyLocalCache("test"));
		LongKeyLocalCache cache = (LongKeyLocalCache) manager.pollCache("test");
		cache.put(1l, "a", 10);
		Thread.sleep(5);
		System.out.println(cache.get(1l));
	}
}
