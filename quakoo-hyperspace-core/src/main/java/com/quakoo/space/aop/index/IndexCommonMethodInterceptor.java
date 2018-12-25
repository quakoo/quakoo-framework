package com.quakoo.space.aop.index;

import com.quakoo.space.IndexBaseDao;
import com.quakoo.space.annotation.index.IndexDaoMethod;
import com.quakoo.space.annotation.index.IndexSort;
import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.enums.index.IndexMethodEnum;
import com.quakoo.space.model.FieldInfo;
import com.quakoo.space.aop.jdbc.JdbcCommonMethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexCommonMethodInterceptor extends JdbcCommonMethodInterceptor {

    private String formatCacheKey(String cacheKey, Object object, Method method, Object[] args) {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
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
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        Map<Method, String> map = dao.getUnformateCacheKeys();
        String cacheKey = map.get(method);
        cacheKey = this.formatCacheKey(cacheKey, object, method, args);
        return cacheKey;
    }

    private String getSql(Object object, Method method, Object[] args, long shardingId) throws Throwable {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        Map<Method, String> map = dao.getListJdbcSqls();
        String sql = map.get(method);
        sql = String.format(sql, dao.getTable(shardingId));
        return sql;
    }

    private List<Object> getparams(Object object, Method method, Object[] args) {
        List<Object> result = new ArrayList<Object>();
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        Map<Integer, String> params = dao.getParamsIndexMap().get(method);
        for (int i : params.keySet()) {
            result.add(args[i]);
        }
        return result;
    }

    private double getCursor(Object object, Method method, Object[] args) {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        int cursor_index = dao.getCursorIndex().get(method);
        double cursor = (double) args[cursor_index];
        return cursor;
    }

    private int getSize(Object object, Method method, Object[] args) {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        int size_index = dao.getSizeIndex().get(method);
        int size = (Integer) args[size_index];
        return size;
    }

    private String getSearchValue(Object object, Method method, Object[] args) {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        int size_index = dao.getSearchIndex().get(method);
        String res = (String) args[size_index];
        return res;
    }

    private CacheSortOrder getCache_sort_order(Method method) {
        IndexDaoMethod indexDaoMethod = method.getAnnotation(IndexDaoMethod.class);
        CacheSortOrder res = indexDaoMethod.order();
        return res;
    }

    private FieldInfo getSortFieldInfo(Object object, Method method) {
        IndexSort indexSort = method.getAnnotation(IndexSort.class);
        if(null == indexSort) throw new IllegalArgumentException("IndexSort is null");
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        FieldInfo res = dao.getIndexSortFieldInfos().get(indexSort.value());
        return res;
    }

    private Object handleGetIndexes(Object object, Method method, Object[] args) throws Throwable {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        String cacheKey = this.getCacheKey(object, method, args);
        String sql = this.getSql(object, method, args, 0);
        List<Object> params = this.getparams(object, method, args);
        Object[] sqlParams = params.toArray();
        CacheSortOrder cache_sort_order = getCache_sort_order(method);
        FieldInfo sortFieldInfo = getSortFieldInfo(object, method);
        int size = getSize(object, method, args);
        double cursor = getCursor(object, method, args);
        return dao.indexList(sortFieldInfo, cacheKey, cache_sort_order, sql, cursor, size, sqlParams);
    }

    private Object handleSearchIndexes(Object object, Method method, Object[] args) throws Throwable {
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        List<Object> params = this.getparams(object, method, args);
        String sql = this.getSql(object, method, args, 0);
        Object[] sqlParams = params.toArray();
        CacheSortOrder cache_sort_order = getCache_sort_order(method);
        int size = getSize(object, method, args);
        double cursor = getCursor(object, method, args);
        String searchValue = getSearchValue(object, method, args);
        return dao.searchList(cache_sort_order, searchValue, sql, cursor, size, sqlParams);
    }

    private Object handleInsertIndex(Object object, Method method, Object[] args) throws Throwable {
        if(args.length > 1) throw new IllegalArgumentException("insert method param's size is more then one");
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        Object res = dao.insertIndex(args[0]);
        return res;
    }

    private Object handleLoadIndex(Object object, Method method, Object[] args) throws Throwable {
        if(args.length > 1) throw new IllegalArgumentException("load method param's size is more then one");
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        Object res = dao.loadIndex(args[0]);
        return res;
    }

    private Object handleDeleteIndex(Object object, Method method, Object[] args) throws Throwable {
        if(args.length > 1) throw new IllegalArgumentException("delete method param's size is more then one");
        IndexBaseDao<?> dao = (IndexBaseDao<?>) object;
        Object res = dao.deleteIndex(args[0]);
        return res;
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        IndexDaoMethod indexDaoMethod = method.getAnnotation(IndexDaoMethod.class);
        if (null != indexDaoMethod) {
            IndexMethodEnum methodEnum = indexDaoMethod.methodEnum();
            if(methodEnum == IndexMethodEnum.getIndexes) return this.handleGetIndexes(object, method, args);
            else if(methodEnum == IndexMethodEnum.insertIndex) return this.handleInsertIndex(object, method, args);
            else if(methodEnum == IndexMethodEnum.loadIndex) return this.handleLoadIndex(object, method, args);
            else if(methodEnum == IndexMethodEnum.deleteIndex) return this.handleDeleteIndex(object, method, args);
            else if(methodEnum == IndexMethodEnum.searchIndexes) return this.handleSearchIndexes(object, method, args);
        }
        return methodProxy.invokeSuper(object, args);
    }
}
