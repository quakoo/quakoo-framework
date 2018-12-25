package com.quakoo.space;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.space.annotation.HyperspaceCombinationId;
import com.quakoo.space.annotation.HyperspaceId;
import com.quakoo.space.annotation.HyperspacePrimaryId;
import com.quakoo.space.annotation.index.IndexDaoMethod;
import com.quakoo.space.annotation.index.IndexMethodParam;
import com.quakoo.space.annotation.index.IndexSearch;
import com.quakoo.space.annotation.index.IndexSort;
import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.enums.index.IndexMethodEnum;
import com.quakoo.space.enums.index.IndexMethodParamEnum;
import com.quakoo.space.model.FieldInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PermissionDeniedDataAccessException;

import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class IndexBaseDao<T> extends JdbcBaseDao<T> implements InitializingBean {

    public static final String cacheKeysign = "__index__";

    protected int DEFAULT_MAX_LIST_SIZE = 100000;// 最大的list的值

    protected int DEFAULT_EXPIRED_TIME = 60 * 60 * 24 * 2;

    @Autowired(required = true)
    @Qualifier("hyperspaceConfig")
    private HyperspaceConfig hyperspaceConfig;

    @Resource
    private JedisX cache;

    // 主键的对象缓存
    protected String objectCacheKey;

    // 组合键的对象缓存
    protected String objectCacheCombinationKey;

    private Map<Method, String> unformateCacheKeys = Maps.newHashMap();

    private Map<String, List<String>> filedCacheKeys = Maps.newHashMap();

    private Map<String, FieldInfo> indexSortFieldInfos = Maps.newHashMap();

    private Map<Method, String> listJdbcSqls = Maps.newHashMap();

    private Map<Method, Map<Integer, String>> paramsIndexMap = Maps.newHashMap();

    private Map<Method, Integer> sizeIndex = Maps.newHashMap();

    private Map<Method, Integer> cursorIndex = Maps.newHashMap();

    private Map<Method, Integer> searchIndex = Maps.newHashMap();

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        String projectName = getHyperspaceConfig().getProjectName();
        String objectCacheKey = super.entityClass.getName();
        objectCacheKey = projectName + "_object_" + objectCacheKey;
        if (super.primaryFieldInfo != null) {
            this.objectCacheKey = objectCacheKey + generateCacheKey(super.primaryFieldInfo);
        }
        this.objectCacheCombinationKey = objectCacheKey;
        if (super.combinationFieldInfos.size() > 0) {
            for (FieldInfo fieldInfo : combinationFieldInfos) {
                this.objectCacheCombinationKey = objectCacheCombinationKey + generateCacheKey(fieldInfo) + "_";
            }
        }

        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            IndexDaoMethod daoMethod = method.getAnnotation(IndexDaoMethod.class);
            if (null != daoMethod && (daoMethod.methodEnum() == IndexMethodEnum.getIndexes || daoMethod.methodEnum() == IndexMethodEnum.searchIndexes)) {
                IndexSort indexSort = method.getAnnotation(IndexSort.class);
                if(null != indexSort) {
                    int sizeIdx = getParamsIndex(method, IndexMethodParamEnum.size);
                    if (sizeIdx >= 0) {
                        sizeIndex.put(method, sizeIdx);
                    } else {
                        throw new IllegalArgumentException("IndexMethodParamEnum size is null");
                    }
                    int cursorIdx = getParamsIndex(method, IndexMethodParamEnum.cursor);
                    if (cursorIdx >= 0) {
                        cursorIndex.put(method, cursorIdx);
                    } else {
                        throw new IllegalArgumentException("IndexMethodParamEnum cursor is null");
                    }

                    LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
                    paramsIndexMap.put(method, fieldNames);

                    if(daoMethod.methodEnum() == IndexMethodEnum.searchIndexes) {
                        IndexSearch indexSearch = method.getAnnotation(IndexSearch.class);
                        if(null == indexSearch) {
                            throw new IllegalArgumentException("IndexSearch is null");
                        }
                        int searchIdx = getParamsIndex(method, IndexMethodParamEnum.search);
                        if (searchIdx >= 0) {
                            searchIndex.put(method, searchIdx);
                        } else {
                            throw new IllegalArgumentException("IndexMethodParamEnum search is null");
                        }

                        String sortValue = daoMethod.order().name();
                        String sortName = indexSort.value();
                        String searchFileName = indexSearch.value();
                        String sortSign = " < ";
                        if (sortValue.equals("asc")) {
                            sortSign = " > ";
                        }
                        String whereSql = " where ";
                        for (int i : fieldNames.keySet()) {
                            String fieldName = fieldNames.get(i);
                            FieldInfo fieldInfo = getFieldInfoByName(fieldName);
                            if (fieldInfo == null) {
                                throw new RuntimeException("方法参数不正确,必须和对应的domain名字一样，或者加上注解标识：class:"
                                        + method.getDeclaringClass().getName() + ",method:" + method.getName() + ",field:"
                                        + fieldName);
                            }
                            String dbName = fieldInfo.getDbName();
                            whereSql = whereSql + dbName + " =? and ";
                        }
                        whereSql += searchFileName + " like ? and " + sortName + sortSign + " ? ";

                        String listJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                                + sortName + " " + sortValue + " limit ? ";
                        listJdbcSqls.put(method, listJdbcSql);
                    }

                    if(daoMethod.methodEnum() == IndexMethodEnum.getIndexes) {
                        String sortName = indexSort.value();
                        String cacheKey = projectName + "_" + entityClass.getName() + "_" + sortName + cacheKeysign;
                        for (int i : fieldNames.keySet()) {
                            String fieldName = fieldNames.get(i);
                            cacheKey = cacheKey + generateCacheKey(fieldName);
                        }
                        unformateCacheKeys.put(method, cacheKey);

                        List<String> cacheKeys = filedCacheKeys.get(sortName);
                        if(null == cacheKeys) {
                            cacheKeys = Lists.newArrayList();
                            filedCacheKeys.put(sortName, cacheKeys);
                        }
                        cacheKeys.add(cacheKey);

                        String whereSql = " where ";
                        for (int i : fieldNames.keySet()) {
                            String fieldName = fieldNames.get(i);
                            FieldInfo fieldInfo = getFieldInfoByName(fieldName);
                            if (fieldInfo == null) {
                                throw new RuntimeException("方法参数不正确,必须和对应的domain名字一样，或者加上注解标识：class:"
                                        + method.getDeclaringClass().getName() + ",method:" + method.getName() + ",field:"
                                        + fieldName);
                            }
                            String dbName = fieldInfo.getDbName();
                            whereSql = whereSql + dbName + " =? and ";
                        }
                        if (fieldNames.size() == 0) {
                            whereSql = "";
                        } else {
                            whereSql = whereSql.substring(0, whereSql.length() - 4);
                        }

                        String listJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                                + sortName + " desc limit " + DEFAULT_MAX_LIST_SIZE;
                        listJdbcSqls.put(method, listJdbcSql);
                    }
                } else throw new IllegalArgumentException("IndexSort is null");
            }
        }

        for (PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if (!"class".equals(fieldName)) {
                Field field = ReflectUtil
                        .getFieldByName(fieldName, entityClass);
                if (field != null) {
                    IndexSort sortKey = field.getAnnotation(IndexSort.class);
                    if (null != sortKey) {
                        String name = one.getName();
                        String dbName = name;
                        Method writeMethod = one.getWriteMethod();
                        Method readMethod = one.getReadMethod();

                        FieldInfo fieldInfo = new FieldInfo(field, name,
                                dbName, writeMethod, readMethod);
                        indexSortFieldInfos.put(sortKey.value(), fieldInfo);
                    }
                }
            }
        }
    }

    protected void add_object_cache(T model) {
        String cacheKey = null;
        if (primaryFieldInfo != null) {
            cacheKey = this.get_identity_object_cacheKey(model);
        }
        if (combinationFieldInfos.size() > 0) {
            cacheKey = this.get_combination_object_cacheKey(model);
        }
        if(StringUtils.isNotBlank(cacheKey)) this.cache.setObject(cacheKey, DEFAULT_EXPIRED_TIME, model);
    }

    private String get_combination_object_cacheKey(T param) {
        String cacheKey = this.objectCacheCombinationKey;
        try {
            Object value = null;
            for (FieldInfo fieldInfo : combinationFieldInfos) {
                value = fieldInfo.getReadMethod().invoke(param);
                cacheKey = cacheKey.replaceAll(generateCacheKey(fieldInfo),
                        replaceCacheKeyValue(fieldInfo.getName(), value));
            }
        } catch (Exception e) {
            logger.error(daoClassName + "," + e.getMessage(), e);
        }
        return cacheKey;
    }

    private String get_identity_object_cacheKey(T param) {
        String cacheKey = this.objectCacheKey;
        try {
            Object value = null;
            value = this.primaryFieldInfo.getReadMethod().invoke(param);
            cacheKey = cacheKey.replaceAll(generateCacheKey(primaryFieldInfo),
                    replaceCacheKeyValue(primaryFieldInfo.getName(), value));
        } catch (Exception e) {
            logger.error(daoClassName + "," + e.getMessage(), e);
        }
        return cacheKey;
    }

    public Void cache_addOne(T model) {
        for(Map.Entry<String, List<String>> entry : filedCacheKeys.entrySet()) {
            String sortName = entry.getKey();
            List<String> unformateCacheKeys = entry.getValue();
            for(String unformateCacheKey : unformateCacheKeys) {
                String cacheKey = formartCacheKey(model, unformateCacheKey);
                cache_addOne(sortName, cacheKey, model);
            }
        }
        return null;
    }

    public Void cache_deleteOne(T model) {
        for(Map.Entry<String, List<String>> entry : filedCacheKeys.entrySet()) {
            List<String> unformateCacheKeys = entry.getValue();
            for(String unformateCacheKey : unformateCacheKeys) {
                String cacheKey = formartCacheKey(model, unformateCacheKey);
                cache_deleteOne(cacheKey, model);
            }
        }
        return null;
    }

    public Void cache_deleteOne(String cacheKey, T model) {
        try {
            if (cacheKey != null && this.cache.exists(cacheKey)) {
                this.cache.zremObject(cacheKey, model);
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        return null;
    }


    public Void cache_addOne(String sortName, String cacheKey, T model) {
        try {
            FieldInfo indexInfo = indexSortFieldInfos.get(sortName);
            if (cacheKey != null && this.cache.exists(cacheKey)) {
                double cacheSort = Double.valueOf(indexInfo.getReadMethod().invoke(model).toString());
                this.cache.zaddObject(cacheKey, cacheSort, model);

                long count = this.cache.zcard(cacheKey);
                if(count > DEFAULT_MAX_LIST_SIZE) {
                    int num = (int) count - (DEFAULT_MAX_LIST_SIZE) - 1;
                    this.cache.zremrangeByRank(cacheKey, 0, num);
                }
            } else {
                cache.delete(decideIsNullListCacheKey(cacheKey));
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        return null;
    }

    public T insertIndex(Object obj) throws DataAccessException {
        try {
            T t = (T) obj;
            t = jdbc_insert(t);
            this.add_object_cache(t);
            this.cache_addOne(t);
            return t;
        } catch (Exception e) {
            throw new PermissionDeniedDataAccessException("", e);
        }
    }

    public T loadIndex(Object obj) throws DataAccessException {
        if (obj instanceof HyperspaceId) {
            return load((HyperspaceId) obj);
        } else if (obj instanceof Long) {
            return load_identity((Long) obj);
        } else if (obj.getClass().getName().equals(entityClass.getName())) {
            try {
                return load(getHyperspaceIdByEntity((T) obj));
            } catch (Exception e) {
                throw new RuntimeException("", e);
            }
        } else {
            throw new RuntimeException("not support:" + obj.getClass() + ",entityClass:" + entityClass);
        }
    }

    public boolean deleteIndex(Object obj) throws DataAccessException {
        if (obj instanceof HyperspaceId) {
            return delete((HyperspaceId) obj);
        } else if (obj instanceof Long) {
            return delete_identity((Long) obj);
        } else if (obj.getClass().getName().equals(entityClass.getName())) {
            try {
                return delete(getHyperspaceIdByEntity((T) obj));
            } catch (Exception e) {
                throw new RuntimeException("", e);
            }
        } else {
            throw new RuntimeException("not support:" + obj.getClass() + ",entityClass:" + entityClass);
        }
    }

    protected boolean delete(HyperspaceId hyperspaceId) throws DataAccessException {
        if (hyperspaceId instanceof HyperspacePrimaryId) {
            HyperspacePrimaryId hyperspacePrimaryId = (HyperspacePrimaryId) hyperspaceId;
            return delete_identity(hyperspacePrimaryId.getId(), hyperspacePrimaryId.getSharding());
        } else {
            try {
                HyperspaceCombinationId hyperspaceCombinationId = (HyperspaceCombinationId) hyperspaceId;
                if (hyperspaceCombinationId.getSharding() == 0) {
                    return delete_combination(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId));
                } else {
                    return delete_combination(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId),
                            hyperspaceCombinationId.getSharding());
                }
            } catch (Exception e) {
                throw new PermissionDeniedDataAccessException("", e);
            }

        }
    }

    protected boolean delete_combination(T param, long... sharding) throws DataAccessException {
        boolean ret = false;
        T model = this.load_combination(param, sharding);
        if (sharding == null || sharding.length == 0) {
            ret = super.jdbc_delete_combination(param);
        } else {
            ret = super.jdbc_delete_combination(param, sharding[0]);
        }
        if (ret) {
            this.delete_object_cache(model);
            cache_deleteOne(model);
        }
        return ret;
    }

    protected boolean delete_identity(long id, long... sharding) throws DataAccessException {
        boolean ret = false;
        T model = this.load_identity(id, sharding);
        if (sharding == null || sharding.length == 0) {
            ret = super.jdbc_delete_identity(id);
        } else {
            ret = super.jdbc_delete_identity(id, sharding[0]);
        }
        if (ret) {
            this.delete_object_cache(model);
            cache_deleteOne(model);
        }
        return ret;
    }

    protected void delete_object_cache(T model) {
        String cacheKey = null;
        if (primaryFieldInfo != null) {
            cacheKey = this.get_identity_object_cacheKey(model);
            this.cache.delete(cacheKey);
        }
        if (combinationFieldInfos.size() > 0) {
            cacheKey = this.get_combination_object_cacheKey(model);
            this.cache.delete(cacheKey);
        }
    }

    protected T load(HyperspaceId hyperspaceId) throws DataAccessException {
        if (hyperspaceId instanceof HyperspacePrimaryId) {
            HyperspacePrimaryId hyperspacePrimaryId = (HyperspacePrimaryId) hyperspaceId;
            return load_identity(hyperspacePrimaryId.getId(), hyperspacePrimaryId.getSharding());
        } else {
            try {
                HyperspaceCombinationId hyperspaceCombinationId = (HyperspaceCombinationId) hyperspaceId;
                if (hyperspaceCombinationId.getSharding() == 0) {
                    return load_combination(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId));
                } else {
                    return load_combination(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId),
                            hyperspaceCombinationId.getSharding());
                }
            } catch (Exception e) {
                throw new PermissionDeniedDataAccessException("", e);
            }

        }
    }

    protected T load_combination(T param, long... sharding) {
        if (isShardingTable && !combinationFieldInfos.contains(shardingFieldInfo)
                && (sharding == null || sharding.length == 0)) {
            throw new IllegalAccessError("找不到分表字段");
        }
        T model = null;
        String cacheKey = this.get_combination_object_cacheKey(param);
        model = this.hit_object_cache(cacheKey);
        if (model != null) {
            return model;
        }
        if (sharding == null || sharding.length == 0) {
            model = super.jdbc_load_combination(param);
        } else {
            model = super.jdbc_load_combination(param, sharding[0]);
        }
        if (model != null) {
            this.add_object_cache(model);
        }
        return model;
    }

    protected T load_identity(long id, long... sharding) {
        if (isShardingTable && shardingFieldInfo != primaryFieldInfo && (sharding == null || sharding.length == 0)) {
            throw new IllegalAccessError("找不到分表字段");
        }
        T model = null;
        String cacheKey = this.get_identity_object_cacheKey(id);
        model = this.hit_object_cache(cacheKey);
        if (model != null) {
            return model;
        }
        if (sharding == null || sharding.length == 0) {
            model = jdbc_load_identity(id);
        } else {
            model = jdbc_load_identity(id, sharding[0]);
        }
        if (model != null) {
            this.add_object_cache(model);
        }
        return model;
    }

    protected T hit_object_cache(String cacheKey) {
        T model = (T) cache.getObject(cacheKey, null);
        if(null != model) this.cache.expire(cacheKey, DEFAULT_EXPIRED_TIME);
        return model;
    }

    public T createParamModelByHyperspaceCombinationId(HyperspaceCombinationId hyperspaceCombinationId)
            throws Exception {
        T model = (T) entityClass.newInstance();
        boolean hasShardingFieldInfo = false;
        if (combinationFieldInfos.size() > 0) {
            for (int i = 0; i < combinationFieldInfos.size(); i++) {
                FieldInfo fieldInfo = combinationFieldInfos.get(i);
                fieldInfo.getWriteMethod().invoke(model, hyperspaceCombinationId.getList().get(i));
                if (fieldInfo == shardingFieldInfo) {
                    hasShardingFieldInfo = true;
                }
            }
            if (!hasShardingFieldInfo) {
                shardingFieldInfo.getWriteMethod().invoke(model, hyperspaceCombinationId.getSharding());
            }
        }
        return model;
    }

    protected String get_identity_object_cacheKey(long id) {
        String cacheKey = this.objectCacheKey;
        try {
            cacheKey = cacheKey.replace(generateCacheKey(primaryFieldInfo),
                    replaceCacheKeyValue(primaryFieldInfo.getName(), String.valueOf(id)));
        } catch (Exception e) {
            logger.error(daoClassName + "," + e.getMessage(), e);
        }
        return cacheKey;
    }

    public List<T> searchList(CacheSortOrder order, String searchValue, String sql, double cursor, int size, Object[] sqlParams) throws DataAccessException {
        if(order == CacheSortOrder.desc) cursor = (cursor == 0) ? Double.MAX_VALUE : cursor;
        List<Object> params = Lists.newArrayList(sqlParams);
        params.add("%" + searchValue + "%");
        params.add(cursor);
        params.add(size);
        sqlParams = params.toArray();
        List<T> res = super.jdbc_getList(0, sql, sqlParams);
        return res;
    }

    public List<T> indexList(FieldInfo sortFieldInfo, String cacheKey, CacheSortOrder order,
                             String sql, double cursor, int size, Object[] sqlParams) throws DataAccessException {
        List<T> result = Lists.newArrayList();
        double minScore = 0, maxScore = 0;
        if(!cache.exists(decideIsNullListCacheKey(cacheKey))) {
            Set<Object> set = null;
            if (order == CacheSortOrder.asc) {
                minScore = cursor;
                maxScore = Double.MAX_VALUE;
                set = this.cache.zrangeByScoreObject(cacheKey, minScore, maxScore, 0, size, null);
            } else {
                maxScore = (cursor == 0) ? Double.MAX_VALUE : cursor;
                set = this.cache.zrevrangeByScoreObject(cacheKey, maxScore, minScore, 0, size, null);
            }
            if(null == set || set.size() == 0) {
                List<T> list = super.jdbc_getList(0, sql, sqlParams);
                if (list != null && list.size() > 0) {
                    try {
                        this._addListCache(list, cacheKey, sortFieldInfo);
                    } catch (Exception e) {
                        throw new PermissionDeniedDataAccessException("", e);
                    }
                    if (order == CacheSortOrder.asc) {
                        set = this.cache.zrangeByScoreObject(cacheKey, minScore, maxScore, 0, size, null);
                    } else {
                        set = this.cache.zrevrangeByScoreObject(cacheKey, maxScore, minScore, 0, size, null);
                    }
                    for(Object one : set) {
                        result.add((T) one);
                    }
                } else {
                    cache.setString(decideIsNullListCacheKey(cacheKey), DEFAULT_EXPIRED_TIME, "true");
                }
            } else {
                for(Object one : set) {
                    result.add((T) one);
                }
            }
        }
        return result;
    }

    public HyperspaceId getHyperspaceIdByEntity(T model) throws Exception {
        if (primaryFieldInfo != null) {
            long id = ReflectUtil.getLongValueForLongOrInt(primaryFieldInfo.getReadMethod().invoke(model));
            if (id > 0) {
                HyperspacePrimaryId hyperspacePrimaryId = new HyperspacePrimaryId(id);
                if (primaryFieldInfo != shardingFieldInfo) {
                    hyperspacePrimaryId.setSharding(ReflectUtil.getLongValueForLongOrInt(shardingFieldInfo
                            .getReadMethod().invoke(model)));
                }
                return hyperspacePrimaryId;
            }
        }
        boolean hasShardingFieldInfo = false;
        if (combinationFieldInfos.size() > 0) {
            HyperspaceCombinationId hyperspaceCombinationId = new HyperspaceCombinationId();
            for (FieldInfo fieldInfo : combinationFieldInfos) {
                hyperspaceCombinationId.getList().add(fieldInfo.getReadMethod().invoke(model));
                if (fieldInfo == shardingFieldInfo) {
                    hasShardingFieldInfo = true;
                }
            }
            if (!hasShardingFieldInfo) {
                hyperspaceCombinationId.setSharding(ReflectUtil.getLongValueForLongOrInt(shardingFieldInfo
                        .getReadMethod().invoke(model)));
            }
            return hyperspaceCombinationId;
        }

        return null;
    }

    private void _addListCache(List<T> list, String cacheKey, FieldInfo sortFieldInfo) throws Exception {
        if (list != null && list.size() > 0) {
            Map<Object, Double> members = new HashMap<Object, Double>();
            for (T one : list) {
                Double cache_sort = 0d;
                try {
                    cache_sort = Double.valueOf((sortFieldInfo.getReadMethod().invoke(one)).toString());
                } catch (Exception e) {
                    throw new PermissionDeniedDataAccessException("", e);
                }
                members.put(one, cache_sort);
            }
            this.cache.zaddMultiObject(cacheKey, members);
            this.cache.expire(cacheKey, DEFAULT_EXPIRED_TIME);
        }
    }

    private int getParamsIndex(Method method, IndexMethodParamEnum paramEnum) {
        int index = -1;
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                Annotation one = annotations[i][0];
                if (one instanceof IndexMethodParam) {
                    IndexMethodParam param = (IndexMethodParam) one;
                    if (param.paramEnum() == paramEnum) {
                        index = i;
                    }
                }
            }
        }
        return index;
    }

    public String getIsNullListCacheKey(String cachekey) {
        return decideIsNullListCacheKey(cachekey);
    }

    private String decideIsNullListCacheKey(String decidedCacheKey) {
        return decidedCacheKey + "_isNull";
    }

    public String generateCacheKey(String name) {
        return "_" + name + "_@" + name;
    }

    private LinkedHashMap<Integer, String> getParamsFields(Method method) {
        LinkedHashMap<Integer, String> list = new LinkedHashMap<Integer, String>();
        String[] result = ReflectUtil.getMethodParamNames(method);
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                Annotation one = annotations[i][0];
                if (one instanceof IndexMethodParam) {
                        result[i] = "";
                }
            }
        }

        for (int i = 0; i < result.length; i++) {
            String s = result[i];
            if (StringUtils.isNotBlank(s)) {
                list.put(i, s);
            }
        }

        return list;
    }

    private String formartCacheKey(T model, String cacheKey) {
        for (FieldInfo fieldInfo : fields) {
            try {
                cacheKey = cacheKey.replaceAll(generateCacheKey(fieldInfo.getName()),
                        replaceCacheKeyValue(fieldInfo.getName(), fieldInfo.getReadMethod().invoke(model)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return cacheKey;
    }

    private String generateCacheKey(FieldInfo fieldInfo) {
        return "_" + fieldInfo.getName() + "_@" + fieldInfo.getName();
    }

    public String replaceCacheKeyValue(String name, Object value) {
        if (null == value) {
            value = "NULL";
        }
        return "_" + name + "_" + value;
    }

    private FieldInfo getFieldInfoByName(String name) {
        for (FieldInfo fieldInfo : fields) {
            if (fieldInfo.getName().equals(name)) {
                return fieldInfo;
            }
        }
        return null;
    }

    public Map<Method, String> getUnformateCacheKeys() {
        return unformateCacheKeys;
    }

    public void setUnformateCacheKeys(Map<Method, String> unformateCacheKeys) {
        this.unformateCacheKeys = unformateCacheKeys;
    }

    public Map<String, List<String>> getFiledCacheKeys() {
        return filedCacheKeys;
    }

    public void setFiledCacheKeys(Map<String, List<String>> filedCacheKeys) {
        this.filedCacheKeys = filedCacheKeys;
    }

    public Map<String, FieldInfo> getIndexSortFieldInfos() {
        return indexSortFieldInfos;
    }

    public void setIndexSortFieldInfos(Map<String, FieldInfo> indexSortFieldInfos) {
        this.indexSortFieldInfos = indexSortFieldInfos;
    }

    public Map<Method, String> getListJdbcSqls() {
        return listJdbcSqls;
    }

    public void setListJdbcSqls(Map<Method, String> listJdbcSqls) {
        this.listJdbcSqls = listJdbcSqls;
    }

    public Map<Method, Map<Integer, String>> getParamsIndexMap() {
        return paramsIndexMap;
    }

    public void setParamsIndexMap(Map<Method, Map<Integer, String>> paramsIndexMap) {
        this.paramsIndexMap = paramsIndexMap;
    }

    public Map<Method, Integer> getSizeIndex() {
        return sizeIndex;
    }

    public void setSizeIndex(Map<Method, Integer> sizeIndex) {
        this.sizeIndex = sizeIndex;
    }

    public Map<Method, Integer> getCursorIndex() {
        return cursorIndex;
    }

    public void setCursorIndex(Map<Method, Integer> cursorIndex) {
        this.cursorIndex = cursorIndex;
    }

    public HyperspaceConfig getHyperspaceConfig() {
        return hyperspaceConfig;
    }

    public void setHyperspaceConfig(HyperspaceConfig hyperspaceConfig) {
        this.hyperspaceConfig = hyperspaceConfig;
    }

    public Map<Method, Integer> getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(Map<Method, Integer> searchIndex) {
        this.searchIndex = searchIndex;
    }
}
