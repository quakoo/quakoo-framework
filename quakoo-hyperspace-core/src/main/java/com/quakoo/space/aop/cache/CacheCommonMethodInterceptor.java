package com.quakoo.space.aop.cache;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Lists;
import com.quakoo.space.annotation.HyperspaceId;
import com.quakoo.space.annotation.cache.CacheDaoMethod;
import com.quakoo.space.annotation.cache.CacheSort;
import com.quakoo.space.annotation.jdbc.JdbcDaoMethod;
import com.quakoo.space.enums.cache.CacheMethodEnum;
import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.enums.jdbc.JdbcMethodEnum;
//import net.sf.cglib.proxy.MethodProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakoo.baseFramework.localCache.LongKeyLocalCache;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.baseFramework.thread.NamedThreadFactory;
import com.quakoo.space.AbstractCacheBaseDao;
import com.quakoo.space.annotation.cache.CacheDaoMethod;
import com.quakoo.space.annotation.cache.CacheSort;
import com.quakoo.space.annotation.jdbc.JdbcDaoMethod;
import com.quakoo.space.aop.jdbc.JdbcCommonMethodInterceptor;
import com.quakoo.space.enums.cache.CacheMethodEnum;
import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.enums.jdbc.JdbcMethodEnum;
import org.springframework.cglib.proxy.MethodProxy;

public class CacheCommonMethodInterceptor extends JdbcCommonMethodInterceptor {

    String projectName;

    static Logger logger = LoggerFactory.getLogger(CacheCommonMethodInterceptor.class);

    public static ConcurrentHashMap<MethodAndArg, LongKeyLocalCache> isListHashMap = new ConcurrentHashMap<MethodAndArg, LongKeyLocalCache>();

    public static ConcurrentHashMap<MethodAndArg, LongKeyLocalCache> isNullHashMap = new ConcurrentHashMap<MethodAndArg, LongKeyLocalCache>();

    public static ConcurrentHashMap<Method, MethodDaoInfo> methodhashMap = new ConcurrentHashMap<Method, MethodDaoInfo>();

    public static ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory(
            "CacheCommonMethodInterceptor"));

    static {
        // 清除失效的本地缓存
        new Thread(new Runnable() {

            @Override
            public void run() {
                for (;;) {
                    try {
                        int cleanSize = 0;
                        for (LongKeyLocalCache longKeyLocalCache : isListHashMap.values()) {
                            for (Long key : longKeyLocalCache.getKesSet()) {
                                Object result = longKeyLocalCache.get(key);
                                if (result == null) {
                                    cleanSize = cleanSize + 1;
                                }
                            }
                        }
                        for (LongKeyLocalCache longKeyLocalCache : isNullHashMap.values()) {
                            for (Long key : longKeyLocalCache.getKesSet()) {
                                Object result = longKeyLocalCache.get(key);
                                if (result == null) {
                                    cleanSize = cleanSize + 1;
                                }
                            }
                        }
                        logger.debug("clean longKeyLocalCache success,size:{}", cleanSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(1000 * 60 * 10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    private String formatCacheKey(String cacheKey, Object object, Method method, Object[] args) {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Map<Integer, String> params = dao.getParamsIndexMap().get(method);
        if (params != null) {
            for (int i : params.keySet()) {
                cacheKey = cacheKey.replaceAll(dao.generateCacheKey(params.get(i)),
                        dao.replaceCacheKeyValue(params.get(i), args[i]));
            }
        }
        return cacheKey;
    }

    private String getCacheKey(Object object, Method method, Object[] args) {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Map<Method, String> map = dao.getUnformateCacheKeys();
        String cacheKey = map.get(method);
        cacheKey = this.formatCacheKey(cacheKey, object, method, args);
        return cacheKey;
    }

    private long getShardingId(Object object, Method method, Object[] args) {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        int shardingId_index = dao.getShardingIndex().get(method);
        long shardingId = this.getShardingId(shardingId_index, args);
        return shardingId;
    }

    private String getSql(Object object, Method method, Object[] args, long shardingId) throws Throwable {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Map<Method, String> map = dao.getListjdbcSqls();
        String sql = map.get(method);
        // this.logger.debug("######### sql:" + sql);
        sql = String.format(sql, dao.getTable(shardingId));
        return sql;
    }

    private String getSqlPage(Object object, Method method, Object[] args, long shardingId) throws Throwable {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Map<Method, String> map = dao.getPagelistjdbcSqls();
        String sql = map.get(method);
        sql = String.format(sql, dao.getTable(shardingId));
        return sql;
    }

    private String getSqlCount(Object object, Method method, Object[] args, long shardingId) throws Throwable {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Map<Method, String> map = dao.getCountJdbcSqls();
        String sql = map.get(method);
        sql = String.format(sql, dao.getTable(shardingId));
        return sql;
    }

    private double getCursor(Object object, Method method, Object[] args) {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        int cursor_index = dao.getCursorIndex().get(method);
        double cursor = (double) args[cursor_index];
        return cursor;
    }

    private int getSize(Object object, Method method, Object[] args) {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        int size_index = dao.getSizeIndex().get(method);
        int size = (Integer) args[size_index];
        return size;
    }
    
    private Object getItem(Object object, Method method, Object[] args) {
    	AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        int item_index = dao.getItemIndex().get(method);
        Object item = args[item_index];
        return item;
    }

    private List<Object> getparams(Object object, Method method, Object[] args) {
        List<Object> result = new ArrayList<Object>();
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Map<Integer, String> params = dao.getParamsIndexMap().get(method);
        for (int i : params.keySet()) {
            result.add(args[i]);
        }
        return result;

    }

    public static CacheSortOrder getCache_sort_order(Object object, Method method, Object[] args) {

        CacheSortOrder cache_sort_order = CacheSortOrder.desc;
        CacheSort cacheSort = method.getAnnotation(CacheSort.class);
        if (cacheSort != null) {

            cache_sort_order = cacheSort.order();
        }
        return cache_sort_order;
    }

    private Object decideListReturnResult(Object object, Method method, Object[] args, List<HyperspaceId> hyperspaceIds)
            throws Exception {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) returnType).getActualTypeArguments();
            if (types[0].equals(dao.getEntityClass())) {
                return dao.load(hyperspaceIds);
            }
        }
        return hyperspaceIds;
    }

    public static void initLocalCache(String cacheKey, String isNullCacheKey, AbstractCacheBaseDao<?> dao,
            Object[] newArgs, Method relationMethod, LongKeyLocalCache isListLoaclCache,
            LongKeyLocalCache isNullLoaclCache, Object arg) throws Throwable {
        boolean isNullInCache = dao.getCache().exists(isNullCacheKey);

        if (isNullInCache) {
            isNullLoaclCache.put(ReflectUtil.getLongValueForLongOrInt( arg), 0, 1000 * 60 * 5);
            return;
        }
        boolean isListInCache = dao.getCache().exists(cacheKey);
        if (isListInCache) {
            isListLoaclCache.put(ReflectUtil.getLongValueForLongOrInt( arg), 0, 1000 * 60);
            return;
        }
        // 既没存有list又没存有null，说明没有初始化redis。
        if (!isNullInCache && !isListInCache) {
            relationMethod.invoke(dao, newArgs);
            isNullInCache = dao.getCache().exists(isNullCacheKey);
            if (isNullInCache) {
                isNullLoaclCache.put(ReflectUtil.getLongValueForLongOrInt( arg), 0, 1000 * 60);
                return;
            }
            isListInCache = dao.getCache().exists(cacheKey);
            if (isListInCache) {
                isListLoaclCache.put(ReflectUtil.getLongValueForLongOrInt( arg), 0, 1000 * 60);
            }
        }
    }

    public class batchGetThread implements Runnable {
        Object[] args;

        LongKeyLocalCache isListLoaclCache;

        LongKeyLocalCache isNullLoaclCache;

        int index;

        CacheSortOrder order;

        Method relationMethod;

        AbstractCacheBaseDao<?> dao;

        List<String> cacheKeys;

        Object arg;

        CountDownLatch cdl;

        public batchGetThread(Object[] args, LongKeyLocalCache isListLoaclCache, LongKeyLocalCache isNullLoaclCache,
                int index, CacheSortOrder order, Method relationMethod, AbstractCacheBaseDao<?> dao,
                List<String> cacheKeys, Object arg, CountDownLatch cdl) {
            super();
            this.args = args;
            this.isListLoaclCache = isListLoaclCache;
            this.isNullLoaclCache = isNullLoaclCache;
            this.index = index;
            this.order = order;
            this.relationMethod = relationMethod;
            this.dao = dao;
            this.cacheKeys = cacheKeys;
            this.arg = arg;
            this.cdl = cdl;
        }

        public batchGetThread(Object[] args, int index, CacheSortOrder order, Method relationMethod, AbstractCacheBaseDao<?> dao,
                              List<String> cacheKeys, Object arg, CountDownLatch cdl) {
            super();
            this.args = args;
            this.index = index;
            this.order = order;
            this.relationMethod = relationMethod;
            this.dao = dao;
            this.cacheKeys = cacheKeys;
            this.arg = arg;
            this.cdl = cdl;
        }

        @Override
        public void run() {
            try {
                Object[] newArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (i == index) {
                        newArgs[i] = arg;
                    } else {
                        newArgs[i] = args[i];
                    }
                }
                String cacheKey = dao.getCacheKey(relationMethod, newArgs, order);
//                String isNullCacheKey = dao.getIsNullListCacheKey(cacheKey);

//                boolean isList = isListLoaclCache.get(ReflectUtil.getLongValueForLongOrInt( arg)) != null;
//                boolean isNull = isNullLoaclCache.get(ReflectUtil.getLongValueForLongOrInt( arg)) != null;
                // 既没存有list又没存有null，说明没有初始化本地cache。

//                boolean isList = dao.getCache().exists(cacheKey);
//                boolean isNull = dao.getCache().exists(isNullCacheKey);
//                if (!isList && !isNull) {
                    relationMethod.invoke(dao, newArgs);
//                    initLocalCache(cacheKey, isNullCacheKey, dao, newArgs, relationMethod, isListLoaclCache,
//                            isNullLoaclCache, arg);
                boolean isList = dao.getCache().exists(cacheKey);
//                    isList = isListLoaclCache.get(ReflectUtil.getLongValueForLongOrInt( arg)) != null;
//                    isNull = isNullLoaclCache.get(ReflectUtil.getLongValueForLongOrInt( arg)) != null;
//                }

                if (isList) {
                    cacheKeys.add(cacheKey);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                cdl.countDown();
            }

        }

    }

    private Object handleGetMergeList(Object object, Method method, Object[] args) throws Throwable {
    	AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
    	int index = dao.getMergeRelationListArgIndex().get(method);
    	MethodAndArg methodAndArg=new MethodAndArg();
    	for(int i=0;i<args.length;i++){
    		if(i!=index){
    			methodAndArg.getArg().add(args[i]);
    		}
    	}
    	
    	
//        if (isListHashMap.get(methodAndArg) == null) {
//            isListHashMap.put(methodAndArg, new LongKeyLocalCache(method.getName()));
//        }
//        if (isNullHashMap.get(methodAndArg) == null) {
//            isNullHashMap.put(methodAndArg, new LongKeyLocalCache(method.getName()));
//        }

//        LongKeyLocalCache isListLoaclCache = isListHashMap.get(methodAndArg);
//        LongKeyLocalCache isNullLoaclCache = isNullHashMap.get(methodAndArg);

      

        Method relationMethod = dao.getMergeRelationMethod().get(method);
        
        List mergeListarg = (List) args[index];
        CacheSortOrder order = getCache_sort_order(object, relationMethod, args);
        List<String> cacheKeys = new CopyOnWriteArrayList<String>();
//        long start = System.currentTimeMillis();

//        if (methodhashMap.get(method) == null) {
//            methodhashMap.put(method, new MethodDaoInfo(dao, args, index));
//        }
//         SyncLocalCacheThread.initThread();

        List<String> keys = Lists.newArrayList();
        for (Object arg : mergeListarg) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (i == index) {
                    newArgs[i] = arg;
                } else {
                    newArgs[i] = args[i];
                }
            }
            String cacheKey = dao.getCacheKey(relationMethod, newArgs, order);
            String isNullCacheKey = dao.getIsNullListCacheKey(cacheKey);
            keys.add(cacheKey);
            keys.add(isNullCacheKey);
        }
        Map<String, Boolean> keyExistsMap = dao.getCache().pipExists(keys);
        List handleList = Lists.newArrayList();
        for(Object arg : mergeListarg) {
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (i == index) {
                    newArgs[i] = arg;
                } else {
                    newArgs[i] = args[i];
                }
            }
            String cacheKey = dao.getCacheKey(relationMethod, newArgs, order);
            String isNullCacheKey = dao.getIsNullListCacheKey(cacheKey);
            boolean isList = keyExistsMap.get(cacheKey);
            boolean isNull = keyExistsMap.get(isNullCacheKey);
            if (!isList && !isNull) {
                handleList.add(arg);
            }
            if(isList) {
                cacheKeys.add(cacheKey);
            }
        }
        if(handleList.size() > 0) {
            CountDownLatch cdl = new CountDownLatch(handleList.size());
            for (Object arg : handleList) {
                executor.execute(new batchGetThread(args, index, order, relationMethod,
                        dao, cacheKeys, arg, cdl));
            }
            cdl.await();
        }
//        CountDownLatch cdl = new CountDownLatch(mergeListarg.size());
//        for (Object arg : mergeListarg) {
////            executor.execute(new batchGetThread(args, isListLoaclCache, isNullLoaclCache, index, order, relationMethod,
////                    dao, cacheKeys, arg, cdl));
//
//            executor.execute(new batchGetThread(args, index, order, relationMethod,
//                    dao, cacheKeys, arg, cdl));
//        }
//        cdl.await();
        int size = getSize(object, method, args);
        List<HyperspaceId> hyperspaceIds = dao.cache_getMergeList(cacheKeys, size, order);
        return decideListReturnResult(object, method, args, hyperspaceIds);

    }

    private Object handleCacheGetList(Object object, Method method, Object[] args, boolean isSharding) throws Throwable {

        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        String countCacheKey = cacheKey.replaceFirst(AbstractCacheBaseDao.listCacheKeysign,
                AbstractCacheBaseDao.countCacheKeysign);
        long shardingId = 0;
        if(isSharding){
        	shardingId = this.getShardingId(object, method, args);
        }
        int size = getSize(object, method, args);
        String sql = this.getSql(object, method, args, shardingId);
        String countSql = this.getSqlCount(object, method, args, shardingId);
        List<Object> params = this.getparams(object, method, args);
        params.add(size);
        List<Object> countParams = this.getparams(object, method, args);
        CacheSortOrder cache_sort_order = getCache_sort_order(object, method, args);

        logger.debug(method.getDeclaringClass().getSimpleName() + "," + method.getName() + "==============cacheKey:"
                + cacheKey + ",shardingId:" + shardingId + ",sql:" + sql + ",size:" + size + ",params:" + params);
        List<HyperspaceId> hyperspaceIds = dao.cache_getList(cacheKey, countCacheKey, cache_sort_order, shardingId,
                sql, countSql, size, params.toArray(), countParams.toArray(), 0);
        return decideListReturnResult(object, method, args, hyperspaceIds);
    }

    private Object handleCacheGetCount(Object object, Method method, Object[] args, boolean isSharding)
            throws Throwable {

        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        long shardingId = 0;
        if (isSharding) {
            shardingId = this.getShardingId(object, method, args);
        }
        String sql = this.getSqlCount(object, method, args, shardingId);
        List<Object> params = this.getparams(object, method, args);
        Object[] sqlParams = params.toArray();
        logger.debug(method.getDeclaringClass().getSimpleName() + "," + method.getName() + "==============cacheKey:"
                + cacheKey + ",shardingId:" + shardingId + ",sql:" + sql + ",params:" + params);
        return dao.cache_getCount(cacheKey, shardingId, sql, sqlParams);
    }
    
    private Object handleGetRank(Object object, Method method, Object[] args, boolean isSharding) throws Throwable {
    	AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        long shardingId = 0;
        if (isSharding) {
            shardingId = this.getShardingId(object, method, args);
        }
        String sql = this.getSql(object, method, args, shardingId);
        Object item = this.getItem(object, method, args);
        List<Object> params = this.getparams(object, method, args);
        Object[] sqlParams = params.toArray();
        CacheSortOrder cache_sort_order = getCache_sort_order(object, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName() + "," + method.getName() + "==============cacheKey:"
                + cacheKey + ",shardingId:" + shardingId + ",sql:" + sql + ",sqlParams:" + params);
        return dao.cache_getRank(cacheKey, cache_sort_order, shardingId, sql, item, sqlParams);
    }

    private Object handleGetPageList(Object object, Method method, Object[] args, boolean isSharding) throws Throwable {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        long shardingId = 0;
        if (isSharding) {
            shardingId = this.getShardingId(object, method, args);
        }
        String sql = this.getSql(object, method, args, shardingId);
        String sqlPage = this.getSqlPage(object, method, args, shardingId);
        int size = getSize(object, method, args);
        double cursor = getCursor(object, method, args);
        List<Object> params = this.getparams(object, method, args);
        Object[] sqlParams = params.toArray();

        List<Object> pageparams = this.getparams(object, method, args);
        pageparams.add(cursor);
        pageparams.add(size);
        Object[] sqlPageParams = pageparams.toArray();

        CacheSortOrder cache_sort_order = getCache_sort_order(object, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName() + "," + method.getName() + "==============cacheKey:"
                + cacheKey + ",shardingId:" + shardingId + ",sql:" + sql + ",sqlPage:" + sqlPage + ",cursor:" + cursor
                + ",size:" + size + ",sqlParams:" + params + ",pageparams:" + pageparams);
        List<HyperspaceId> hyperspaceIds = dao.cache_getPageList(cacheKey, cache_sort_order, shardingId, sql, sqlPage,
                cursor, size, sqlParams, sqlPageParams);
        return decideListReturnResult(object, method, args, hyperspaceIds);
    }

    private Object handleGetAllList(Object object, Method method, Object[] args) throws Throwable {
        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        long shardingId = this.getShardingId(object, method, args);
        String sql = this.getSql(object, method, args, shardingId);
        List<Object> params = this.getparams(object, method, args);
        Object[] sqlParams = params.toArray();

        CacheSortOrder cache_sort_order = getCache_sort_order(object, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName() + "," + method.getName() + "==============cacheKey:"
                + cacheKey + ",shardingId:" + shardingId + ",sql:" + sql + ",sqlParams:" + params);
        List<HyperspaceId> hyperspaceIds = dao.cache_getAllList(cacheKey, cache_sort_order, shardingId, sql, sqlParams);
        return decideListReturnResult(object, method, args, hyperspaceIds);
    }

    private Object handleGetAllListWithoutSharding(Object object, Method method, Object[] args) throws Throwable {

        AbstractCacheBaseDao<?> dao = (AbstractCacheBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        // long shardingId = this.getShardingId(object, method, args);
        String sql = this.getSql(object, method, args, 0);
        List<Object> params = this.getparams(object, method, args);
        Object[] sqlParams = params.toArray();

        CacheSortOrder cache_sort_order = getCache_sort_order(object, method, args);
        logger.debug(method.getDeclaringClass().getSimpleName() + "," + method.getName() + "==============cacheKey:"
                + cacheKey + ",shardingId:" + 0 + ",sql:" + sql + ",sqlParams:" + params);
        List<HyperspaceId> hyperspaceIds = dao.cache_getAllList(cacheKey, cache_sort_order, 0, sql, sqlParams);
        return decideListReturnResult(object, method, args, hyperspaceIds);
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        CacheDaoMethod cacheDaoMethod = method.getAnnotation(CacheDaoMethod.class);
        JdbcDaoMethod jdbcDaoMethod = method.getAnnotation(JdbcDaoMethod.class);
        if (null != jdbcDaoMethod) {
            JdbcMethodEnum jdbcMethodEnum = jdbcDaoMethod.methodEnum();
            if (jdbcMethodEnum == JdbcMethodEnum.getList) {
                return super.handleGetList(object, method, args);
            } else if (jdbcMethodEnum == JdbcMethodEnum.getCount) {
                return super.handleGetCount(object, method, args);
            } else {
                return methodProxy.invokeSuper(object, args);
            }
        }
        if (null != cacheDaoMethod) {
            CacheMethodEnum methodEnum = cacheDaoMethod.methodEnum();
            if (methodEnum != null && args != null) {
                // logger.debug(method.getDeclaringClass().getSimpleName() + ","
                // + method.getName() + "==============args:"
                // + Arrays.asList(args));
            }
            if (methodEnum == CacheMethodEnum.getList) {
                return this.handleCacheGetList(object, method, args, true);
            } else if(methodEnum == CacheMethodEnum.getListWithoutSharding){
            	return this.handleCacheGetList(object, method, args, false);
            } else if (methodEnum == CacheMethodEnum.getCount) {
                return this.handleCacheGetCount(object, method, args, true);
            } else if (methodEnum == CacheMethodEnum.getCountWithoutSharding) {
                return this.handleCacheGetCount(object, method, args, false);
            } else if (methodEnum == CacheMethodEnum.getPageList) {
                return this.handleGetPageList(object, method, args, true);
            } else if (methodEnum == CacheMethodEnum.getPageListWithoutSharding) {
                return this.handleGetPageList(object, method, args, false);
            } else if (methodEnum == CacheMethodEnum.getAllList) {
                return this.handleGetAllList(object, method, args);
            } else if (methodEnum == CacheMethodEnum.getAllListWithoutSharding) {
                return this.handleGetAllListWithoutSharding(object, method, args);
            } else if (methodEnum == CacheMethodEnum.getMergeList) {
                return this.handleGetMergeList(object, method, args);
            } else if (methodEnum == CacheMethodEnum.getRank) {
            	return this.handleGetRank(object, method, args, true);
            } else {
                return methodProxy.invokeSuper(object, args);
            }
        }
        return methodProxy.invokeSuper(object, args);
    }

}
