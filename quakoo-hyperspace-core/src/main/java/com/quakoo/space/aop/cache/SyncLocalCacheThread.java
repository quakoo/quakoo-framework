package com.quakoo.space.aop.cache;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.quakoo.space.enums.cache.CacheSortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakoo.baseFramework.localCache.LongKeyLocalCache;
import com.quakoo.space.AbstractCacheBaseDao;


public class SyncLocalCacheThread {

	public static Boolean started=false;
	
	public static int threadNum=10;
	
	public static final Logger logger=LoggerFactory.getLogger(SyncLocalCacheThread.class);
	
	public static LinkedBlockingQueue<SyncLocalCache>  blockingQueue=new LinkedBlockingQueue<SyncLocalCache>();
	
	public  static void initThread(){
		if(started){
			return;
		}
		else{
			synchronized (started) {
				if(started){
					return;
				}
				started=true;
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						for(;;){
							try{
								long start=System.currentTimeMillis();
								long size=0;
								size=size+syncs(CacheCommonMethodInterceptor.isNullHashMap);
								size=size+syncs(CacheCommonMethodInterceptor.isListHashMap);
								if(size<100){
									Thread.sleep(1000);
								}
								
								while(blockingQueue.size()>100){
									Thread.sleep(1000);
								}
								logger.info("syncs=========cache...,size:{},time:{}",size,(System.currentTimeMillis()-start));
								
								
							}catch(Exception e){
								e.printStackTrace();
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}).start();
				
				
				
				
				
				for(int i=0;i<threadNum;i++){
					
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							for(;;){
								try{
									
									SyncLocalCache s=blockingQueue.take();
									CacheCommonMethodInterceptor.initLocalCache(
											s.getCacheKey(), s.getIsNullCacheKey(), 
											s.getDao(), s.getNewArgs(), 
											s.getRelationMethod(), s.getIsListLoaclCache(), 
											s.getIsNullLoaclCache(), s.getArg());
								}catch(Throwable e){
									e.printStackTrace();
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e1) {
										e1.printStackTrace();
									}
								}
							}
						}
					}).start();
					
					
				}
				
				
				
				
			}
		}
		
	}
	
	
	
	public static long  syncs(ConcurrentHashMap<MethodAndArg, LongKeyLocalCache> map){
		long size=0;
		for(MethodAndArg methodAndArg:map.keySet()){
			Method method=methodAndArg.getMethod();
			LongKeyLocalCache longKeyLocalCache=map.get(methodAndArg);
			MethodDaoInfo methodDaoInfo=CacheCommonMethodInterceptor.methodhashMap.get(method);
			Set<Long> ids=longKeyLocalCache.getKesSet();
			
			
			final Object[] args=methodDaoInfo.getArgs();
			final AbstractCacheBaseDao<?> dao = methodDaoInfo.getDao();
			final Method relationMethod=dao.getMergeRelationMethod().get(method);
			final int index=dao.getMergeRelationListArgIndex().get(method);
			
			final CacheSortOrder order=CacheCommonMethodInterceptor.getCache_sort_order(dao, relationMethod, args);
			
			
			for(final Object arg:ids){
				Object[] newArgs=new Object[args.length];
				for(int i=0;i<args.length;i++){
					if(i==index){
						newArgs[i]=arg;
					}else{
						newArgs[i]=args[i];
					}
				}
				String cacheKey=dao.getCacheKey(relationMethod, newArgs, order);
				String isNullCacheKey=dao.getIsNullListCacheKey(cacheKey);
				
				LongKeyLocalCache isListLoaclCache=CacheCommonMethodInterceptor.isListHashMap.get(methodAndArg);
				LongKeyLocalCache isNullLoaclCache=CacheCommonMethodInterceptor.isNullHashMap.get(methodAndArg);
				
				
				size=size+1;
				blockingQueue.add(new SyncLocalCache(cacheKey, isNullCacheKey, dao, newArgs, relationMethod, isListLoaclCache, isNullLoaclCache, arg));
				
			}
			
			
		}
		return size;
	}
	
	
	
	
	
	
	
}
