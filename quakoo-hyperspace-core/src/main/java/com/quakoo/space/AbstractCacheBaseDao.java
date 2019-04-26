package com.quakoo.space;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.space.annotation.HyperspaceAllId;
import com.quakoo.space.annotation.HyperspaceCombinationId;
import com.quakoo.space.annotation.HyperspaceId;
import com.quakoo.space.annotation.HyperspacePrimaryId;
import com.quakoo.space.annotation.cache.CacheDaoMethod;
import com.quakoo.space.annotation.cache.CacheMergerListParam;
import com.quakoo.space.annotation.cache.CacheMethodParam;
import com.quakoo.space.annotation.cache.CacheSort;
import com.quakoo.space.annotation.domain.HyperspaceDomain;
import com.quakoo.space.enums.cache.CacheMethodEnum;
import com.quakoo.space.enums.cache.CacheMethodParamEnum;
import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.mapper.HyperspaceAllIdRowMapper;
import com.quakoo.space.model.FieldInfo;
import com.quakoo.transaction.JedisXUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PermissionDeniedDataAccessException;

import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.redis.JedisXFactory;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.space.annotation.HyperspaceAllId;
import com.quakoo.space.annotation.HyperspaceCombinationId;
import com.quakoo.space.annotation.HyperspaceId;
import com.quakoo.space.annotation.HyperspacePrimaryId;
import com.quakoo.space.annotation.cache.CacheDaoMethod;
import com.quakoo.space.annotation.cache.CacheMergerListParam;
import com.quakoo.space.annotation.cache.CacheMethodParam;
import com.quakoo.space.annotation.cache.CacheSort;
import com.quakoo.space.annotation.domain.HyperspaceDomain;
import com.quakoo.space.enums.cache.CacheMethodEnum;
import com.quakoo.space.enums.cache.CacheMethodParamEnum;
import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.mapper.HyperspaceAllIdRowMapper;
import com.quakoo.space.model.FieldInfo;

/**
 *
 * @author yongbiaoli haoli junju
 *
 * @param <T>
 *
 *            1、需要定义新的CacheMethodEnum类型，用于获取过滤某些类型的数据，如SQL：status != 1。 2013.9.5
 *            junju 2.在增加了timeline的截取之后 ，发现很多方法有共同之处，可以提取出来，包括后来的service层的拦截也可以用
 *            2013.9.10 yongbiaoli
 *            3.在service层增加拦截，自动处理各个主表结构和对应的不同的关联关系。劲量做到关联关系依靠配置就能自动解决。
 *            2013.9.10 yongbiaoli 4.在现有的dao系统给出缓存自动清理，重建的接口。 2013.9.10
 *            yongbiaoli 4.每个list挂一个小list 用来做merger用 2013.12.04 yongbiaoli
 *
 */
public abstract class AbstractCacheBaseDao<T> extends JdbcBaseDao<T> {
    Logger logger = LoggerFactory.getLogger(AbstractCacheBaseDao.class);

    protected int DEFAULT_MAX_LIST_SIZE = 10000;// 最大的list的值

    protected int DEFAULT_INIT_LIST_SIZE = 2000;// 初始最小的list的值,getAll操作的时候的值。

    protected boolean onlyHasDescList = false;// 只有倒序list，如果只有倒序list，
                                              // 当添加元素发现list超过最大的时候，
                                              // 会删除元素，如果是多种序会删除整个list

    protected int DEFAULT_EXPIRED_TIME = 60 * 60 * 24 * 2;

    protected final static int DEFAULT_TEMP_EXPIRED_TIME = 60 * 2;

    @Autowired(required = false)
    @Qualifier("cachePool")
    protected JedisX cache;

    protected String redisAddress = "";

    @Autowired(required = true)
    @Qualifier("hyperspaceConfig")
    protected HyperspaceConfig hyperspaceConfig;

    protected boolean enableObjectCache = true;

    protected int expireSeconds = DEFAULT_EXPIRED_TIME;

    protected int tmpExpireSeconds = DEFAULT_TEMP_EXPIRED_TIME;

    public static final String listCacheKeysign = "__list__";

    public static final String countCacheKeysign = "__count__";

    // 主键的对象缓存
    protected String objectCacheKey;

    // 组合键的对象缓存
    protected String objectCacheCombinationKey;

    // 未被格式化的CacheKey。
    private Map<Method, String> unformateCacheKeys = new HashMap<Method, String>();

    // mergeRelationMethod
    private Map<Method, Method> mergeRelationMethod = new HashMap<Method, Method>();

    // mergeRelationListArgIndex
    private Map<Method, Integer> mergeRelationListArgIndex = new HashMap<Method, Integer>();

    private Map<Method, String> listjdbcSqls = new HashMap<Method, String>();

    private Map<Method, String> pagelistjdbcSqls = new HashMap<Method, String>();

    private Map<Method, String> countJdbcSqls = new HashMap<Method, String>();

    private Map<Method, Integer> shardingIndex = new HashMap<Method, Integer>();

    private Map<Method, Integer> sizeIndex = new HashMap<Method, Integer>();

    private Map<Method, Integer> cursorIndex = new HashMap<Method, Integer>();

    private Map<Method, Integer> itemIndex = new HashMap<Method, Integer>();

    private Map<Method, Map<Integer, String>> paramsIndexMap = new HashMap<Method, Map<Integer, String>>();

    public String getRedisAddress() {
        return redisAddress;
    }

    public void setRedisAddress(String redisAddress) {
        this.redisAddress = redisAddress;
    }

    public Map<Method, Map<Integer, String>> getParamsIndexMap() {
        return paramsIndexMap;
    }

    public Map<Method, Method> getMergeRelationMethod() {
        return mergeRelationMethod;
    }

    public void setMergeRelationMethod(Map<Method, Method> mergeRelationMethod) {
        this.mergeRelationMethod = mergeRelationMethod;
    }

    public void setParamsIndexMap(Map<Method, Map<Integer, String>> paramsIndexMap) {
        this.paramsIndexMap = paramsIndexMap;
    }

    public void setShardingIndex(Map<Method, Integer> shardingIndex) {
        this.shardingIndex = shardingIndex;
    }

    public Map<Method, Integer> getMergeRelationListArgIndex() {
        return mergeRelationListArgIndex;
    }

    public void setMergeRelationListArgIndex(Map<Method, Integer> mergeRelationListArgIndex) {
        this.mergeRelationListArgIndex = mergeRelationListArgIndex;
    }

    public void setSizeIndex(Map<Method, Integer> sizeIndex) {
        this.sizeIndex = sizeIndex;
    }

    public void setCursorIndex(Map<Method, Integer> cursorIndex) {
        this.cursorIndex = cursorIndex;
    }

    public Map<Method, Integer> getShardingIndex() {
        return shardingIndex;
    }

    public Map<Method, Integer> getSizeIndex() {
        return sizeIndex;
    }

    public Map<Method, Integer> getCursorIndex() {
        return cursorIndex;
    }

    public Map<Method, Integer> getItemIndex() {
		return itemIndex;
	}

	public void setItemIndex(Map<Method, Integer> itemIndex) {
		this.itemIndex = itemIndex;
	}

	public Map<Method, String> getListjdbcSqls() {
        return listjdbcSqls;
    }

    public void setListjdbcSqls(Map<Method, String> listjdbcSqls) {
        this.listjdbcSqls = listjdbcSqls;
    }

    public Map<Method, String> getPagelistjdbcSqls() {
        return pagelistjdbcSqls;
    }

    public void setPagelistjdbcSqls(Map<Method, String> pagelistjdbcSqls) {
        this.pagelistjdbcSqls = pagelistjdbcSqls;
    }

    public Map<Method, String> getCountJdbcSqls() {
        return countJdbcSqls;
    }

    public JedisX getCache() {
        return cache;
    }

    public void setCache(JedisX cache) {
        this.cache = cache;
    }

    public void setCountJdbcSqls(Map<Method, String> countJdbcSqls) {
        this.countJdbcSqls = countJdbcSqls;
    }

    public Map<Method, String> getUnformateCacheKeys() {
        return unformateCacheKeys;
    }

    public void setUnformateCacheKeys(Map<Method, String> unformateCacheKeys) {
        this.unformateCacheKeys = unformateCacheKeys;
    }

    public HyperspaceConfig getHyperspaceConfig() {
        return hyperspaceConfig;
    }

    public void setHyperspaceConfig(HyperspaceConfig hyperspaceConfig) {
        this.hyperspaceConfig = hyperspaceConfig;
    }

    /**
     * 插入对象之后操作
     *
     * @param model
     */
    protected abstract void afterInsert(T model);

    /**
     * 插入对象之后操作
     *
     */
    protected abstract void afterUpteDate(T oldModel, T newModel);

    /**
     * increment对象之后操作
     *
     * @author lichao
     */
    protected abstract void afterIncrement(T oldModel, T newModel);

    /**
     * 删除之后的操作
     *
     * @param model
     */
    protected abstract void afterDelete(T model);

    // ===============================初始化=========================================
    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        this.initCache();
        this.init_object_cache();
        this.checkCacheMethod();
        this.setExpireSeconds();
    }

    protected void initCache() {
        if (StringUtils.isNotBlank(redisAddress)) {
            logger.info(daoClassName + "class:{},redisAddress:{}", entityClass.getName(), redisAddress);
            cache = JedisXFactory.getJedisX(redisAddress, 10000);
        }
    }

    protected Map<String, List<String>> cache_map() {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        List<String> list = new ArrayList<String>();
        List<String> count = new ArrayList<String>();
        List<String> object = new ArrayList<String>();
        result.put("list", list);
        result.put("count", count);
        result.put("object", object);
        Set<String> set = new HashSet<String>(unformateCacheKeys.values());
        for (String one : set) {
            if (one.contains("@")) {
                if (one.contains("_list_")) {
                    list.add(one);
                }
                if (one.contains("_count_")) {
                    count.add(one);
                }
            }
        }
        if (objectCacheKey.contains("@")) {
            object.add(objectCacheKey);
        }
        if (objectCacheCombinationKey.contains("@")) {
            object.add(objectCacheCombinationKey);
        }
        return result;
    }

    /**
     * 初始化objectCache,list key
     *
     * @throws Exception
     */
    private void init_object_cache() throws Exception {
        HyperspaceDomain sign = super.entityClass.getAnnotation(HyperspaceDomain.class);
        String projectName = getHyperspaceConfig().getProjectName();
        this.enableObjectCache = sign.cacheObject();
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

        // 生成 cacheKey

        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (null != daoMethod) {
                CacheMethodEnum methodEnum = daoMethod.methodEnum();
                String cacheKey = projectName + "_" + entityClass.getName();

                if (methodEnum == CacheMethodEnum.getMergeList) {
                    LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
                    String relationMethodName = daoMethod.relationMethodName();
                    Class[] classes = daoMethod.relationMethodParams();
                    Method relationMethod = this.getClass().getMethod(relationMethodName, classes);
                    LinkedHashMap<Integer, String> relationFieldNames = getParamsFields(relationMethod);

                    Annotation[][] annotations = method.getParameterAnnotations();
                    int index = -1;
                    for (int i = 0; i < annotations.length; i++) {
                        if (annotations[i].length > 0) {
                            Annotation one = annotations[i][0];
                            if (one instanceof CacheMergerListParam) {
                                index = i;
                                break;
                            }
                        }
                    }
                    if (index == -1) {
                        throw new RuntimeException("不能找到需要merge的集合，需要加上注解");
                    }
                    if (fieldNames.keySet().size() != relationFieldNames.keySet().size()) {
                        throw new RuntimeException("mergelist method params 必须和 relationMethodParams 一致");
                    }

                    String[] mergeParamsNames = fieldNames.values().toArray(new String[0]);
                    String[] relationParamsNames = relationFieldNames.values().toArray(new String[0]);
                    for (int i = 0; i < mergeParamsNames.length; i++) {
                        if (i != index && !mergeParamsNames[i].equals(relationParamsNames[i])) {
                            throw new RuntimeException("mergelist method params 必须和 relationMethodParams 一致");
                        }
                    }
                    mergeRelationMethod.put(method, relationMethod);
                    mergeRelationListArgIndex.put(method, index);
                    continue;

                }

                if (methodEnum == CacheMethodEnum.getList
                		|| methodEnum == CacheMethodEnum.getListWithoutSharding
                		|| methodEnum == CacheMethodEnum.getPageList
                        || methodEnum == CacheMethodEnum.getPageListWithoutSharding
                        || methodEnum == CacheMethodEnum.getAllList
                        || methodEnum == CacheMethodEnum.getAllListWithoutSharding
                        || methodEnum == CacheMethodEnum.getRank) {
                    cacheKey = cacheKey + listCacheKeysign;
                } else if (methodEnum == CacheMethodEnum.getCount
                        || methodEnum == CacheMethodEnum.getCountWithoutSharding) {
                    cacheKey = cacheKey + countCacheKeysign;

                }

                LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
                for (int i : fieldNames.keySet()) {
                    String fieldName = fieldNames.get(i);
                    cacheKey = cacheKey + generateCacheKey(fieldName);
                }
                unformateCacheKeys.put(method, cacheKey);
                logger.info(daoClassName + "class:{},method:{},cacheKey:{}", new Object[] { entityClass.getName(),
                        method.getName(), cacheKey });
            }
        }

        // 找出sharding，size ,cursor对应的参数序列

        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (null != daoMethod) {
                if (shardingFieldInfo != null) {
                    LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
                    for (int i : fieldNames.keySet()) {
                        if (shardingFieldInfo.getName().equals(fieldNames.get(i))) {
                            shardingIndex.put(method, i);
                            logger.info(daoClassName + "class:{},method:{},shardingIndex:{}", new Object[] {
                                    entityClass.getName(), method.getName(), i });
                        }
                    }
                }

                int sizeIdx = getParamsIndex(method, CacheMethodParamEnum.size);
                if (sizeIdx >= 0) {
                    sizeIndex.put(method, sizeIdx);
                }
                int cursorIdx = getParamsIndex(method, CacheMethodParamEnum.cursor);
                if (cursorIdx >= 0) {
                    cursorIndex.put(method, cursorIdx);
                }

                int itemIdx = getParamsIndex(method, CacheMethodParamEnum.item);
                if(itemIdx >= 0){
                	itemIndex.put(method, itemIdx);
                }

            }
        }

        // 生成jdbc语句
        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (null != daoMethod) {
                CacheMethodEnum methodEnum = daoMethod.methodEnum();
                if (methodEnum != CacheMethodEnum.getList
                		&& methodEnum != CacheMethodEnum.getListWithoutSharding
                		&& methodEnum != CacheMethodEnum.getPageList
                        && methodEnum != CacheMethodEnum.getPageListWithoutSharding
                        && methodEnum != CacheMethodEnum.getCount
                        && methodEnum != CacheMethodEnum.getCountWithoutSharding
                        && methodEnum != CacheMethodEnum.getAllList
                        && methodEnum != CacheMethodEnum.getAllListWithoutSharding
                        && methodEnum != CacheMethodEnum.getRank) {
                    continue;
                }

                CacheSort cacheSort = method.getAnnotation(CacheSort.class);
                String sortFiled = null;
                String sortOrder = "desc";
                if (cacheSort != null) {
                    if (cacheSort.order() == CacheSortOrder.asc) {
                        sortOrder = "asc";
                    }
                }
                if (sortFieldInfo != null) {
                    sortFiled = sortFieldInfo.getDbName();
                }

                String listJdbcSql = null;
                String pageListJdbcSql = null;
                String countSql = null;

                String whereSql = " where ";

                LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
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
                paramsIndexMap.put(method, fieldNames);
                if (fieldNames.size() == 0) {
                    whereSql = "";
                } else {
                    whereSql = whereSql.substring(0, whereSql.length() - 4);
                }

                if (methodEnum == CacheMethodEnum.getAllList || methodEnum == CacheMethodEnum.getAllListWithoutSharding) {
                    if ((method.getParameterTypes().length - fieldNames.size()) != 0) {
                        throw new RuntimeException("getAllList方法参数不正确：class:" + method.getDeclaringClass().getName()
                                + ",method:" + method.getName());
                    }
                    listJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                            + sortFiled + " " + sortOrder + " limit " + DEFAULT_INIT_LIST_SIZE;
                } else if(methodEnum == CacheMethodEnum.getRank){
                	if ((method.getParameterTypes().length - fieldNames.size()) != 1) {
                        throw new RuntimeException("getRank方法参数不正确：class:" + method.getDeclaringClass().getName()
                                + ",method:" + method.getName());
                    }
                	listJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                            + sortFiled + " " + sortOrder + " limit " + DEFAULT_INIT_LIST_SIZE;
                } else if (methodEnum == CacheMethodEnum.getList || methodEnum == CacheMethodEnum.getListWithoutSharding) {
                    if ((method.getParameterTypes().length - fieldNames.size()) != 1) {
                        throw new RuntimeException("getList方法参数不正确：class:" + method.getDeclaringClass().getName()
                                + ",method:" + method.getName());
                    }
                    // list必须有size
                    listJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                            + sortFiled + " " + sortOrder + " limit ?";
                    countSql = "select count(*) from %s " + whereSql;
                } else if (methodEnum == CacheMethodEnum.getPageList
                        || methodEnum == CacheMethodEnum.getPageListWithoutSharding) {
                    if ((method.getParameterTypes().length - fieldNames.size()) != 2) {
                        throw new RuntimeException("getPageList方法参数不正确：class:" + method.getDeclaringClass().getName()
                                + ",method:" + method.getName());
                    }
                    // list必须有size和cursor
                    listJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                            + sortFiled + " " + sortOrder + " limit " + DEFAULT_INIT_LIST_SIZE;
                    String sortSign = "<";
                    if (sortOrder.equals("asc")) {
                        sortSign = ">";
                    }
                    if ("".equals(whereSql)) {
                        whereSql = " where ";
                    } else {
                        whereSql = whereSql + " and ";
                    }
                    whereSql = whereSql + sortFiled + sortSign + " ?";
                    pageListJdbcSql = "select " + super.jdbcHelper.getColumns() + " from %s " + whereSql + " order by "
                            + sortFiled + " " + sortOrder + " limit ?";
                } else if (methodEnum == CacheMethodEnum.getCount
                        || methodEnum == CacheMethodEnum.getCountWithoutSharding) {
                    countSql = "select count(*) from %s " + whereSql;
                }

                if (listJdbcSql != null) {
                    listjdbcSqls.put(method, listJdbcSql);
                    logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@" + "class:{},method:{},listJdbcSql:{}",
                            new Object[] { entityClass.getName(), method.getName(), listJdbcSql });
                }
                if (pageListJdbcSql != null) {
                    pagelistjdbcSqls.put(method, pageListJdbcSql);
                    logger.info(daoClassName + "class:{},method:{},pageListJdbcSql:{}",
                            new Object[] { entityClass.getName(), method.getName(), pageListJdbcSql });
                }
                if (countSql != null) {
                    countJdbcSqls.put(method, countSql);
                    logger.info(daoClassName + "class:{},method:{},countSql:{}", new Object[] { entityClass.getName(),
                            method.getName(), countSql });
                }

            }
        }

        logger.info(daoClassName + "class:{},objectCacheKey:{}", new Object[] { entityClass.getName(),
                this.objectCacheKey });
        logger.info(daoClassName + "class:{},objectCacheCombinationKey:{}", new Object[] { entityClass.getName(),
                this.objectCacheCombinationKey });
    }

    private int getParamsIndex(Method method, CacheMethodParamEnum paramEnum) {
        int index = -1;
        String[] result = ReflectUtil.getMethodParamNames(method);
        for (String str : result) {
            if (StringUtils.isBlank(str)) {
                result = ReflectUtil.getMethodParamNames(method);
            }
        }
        for (int i = 0; i < result.length; i++) {
            String paramName = result[i];
            if (paramName.equals(paramEnum.getName())) {
                index = i;
            }
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                Annotation one = annotations[i][0];
                if (one instanceof CacheMethodParam) {
                    CacheMethodParam param = (CacheMethodParam) one;
                    if (param.paramEnum() == paramEnum) {
                        index = i;
                    }
                }
            }
        }
        return index;
    }

    private LinkedHashMap<Integer, String> getParamsFields(Method method) {
        LinkedHashMap<Integer, String> list = new LinkedHashMap<Integer, String>();
        String[] result = ReflectUtil.getMethodParamNames(method);
        for (int i = 0; i < result.length; i++) {
            String paramName = result[i];
            if (paramName.equals("size") || paramName.equals("cursor") || paramName.equals("item")) {
                result[i] = "";
            }
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].length > 0) {
                Annotation one = annotations[i][0];
                if (one instanceof CacheMethodParam) {
                    CacheMethodParam param = (CacheMethodParam) one;
                    if (StringUtils.isNotBlank(param.field())) {
                        result[i] = param.field();
                    }
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

    private FieldInfo getFieldInfoByName(String name) {

        for (FieldInfo fieldInfo : fields) {
            if (fieldInfo.getName().equals(name)) {
                return fieldInfo;
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public String generateCacheKey(String name) {
        return "_" + name + "_@" + name;
    }

    public String replaceCacheKeyValue(String name, Object value) {
        if (null == value) {
            value = "NULL";
        }
        return "_" + name + "_" + value;
    }

    /**
     *
     * @return
     */
    private String generateCacheKey(FieldInfo fieldInfo) {
        return "_" + fieldInfo.getName() + "_@" + fieldInfo.getName();
    }

    /**
     * 检测各个方法的参数
     */
    private void checkCacheMethod() {
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            CacheDaoMethod daoMethod = method.getAnnotation(CacheDaoMethod.class);
            if (null != daoMethod) {
                CacheMethodEnum methodEnum = daoMethod.methodEnum();

                if (methodEnum == CacheMethodEnum.getList
                		|| methodEnum == CacheMethodEnum.getListWithoutSharding
                		|| methodEnum == CacheMethodEnum.getPageList
                        || methodEnum == CacheMethodEnum.getPageListWithoutSharding
                        || methodEnum == CacheMethodEnum.getAllList
                        || methodEnum == CacheMethodEnum.getAllListWithoutSharding
                        || methodEnum == CacheMethodEnum.getRank) {
                    CacheSort cacheSort = method.getAnnotation(CacheSort.class);
                    if (cacheSort == null && sortFieldInfo == null) {
                        throw new IllegalAccessError("list方法 必须要加上CacheSort注解，或者标明sort字段 class:"
                                + this.getClass().getName() + "method: " + method.getName());
                    }
                } else if (methodEnum == CacheMethodEnum.getCount
                        || methodEnum == CacheMethodEnum.getCountWithoutSharding) {
                } else if (methodEnum == CacheMethodEnum.getMergeList) {
                } else {
                    throw new IllegalAccessError("此结构不支持：" + methodEnum + "  class:" + this.getClass().getName()
                            + "method: " + method.getName());
                }

                // 这几个方法必须声明传入shardingId
                if (shardingIndex.get(method) == null
                		&& methodEnum != CacheMethodEnum.getListWithoutSharding
                		&& methodEnum != CacheMethodEnum.getAllListWithoutSharding
                        && methodEnum != CacheMethodEnum.getPageListWithoutSharding
                        && methodEnum != CacheMethodEnum.getCountWithoutSharding
                        && methodEnum != CacheMethodEnum.getMergeList) {
                    throw new IllegalAccessError("list,count方法 必须要传入shardingId， class:" + this.getClass().getName()
                            + "method: " + method.getName());
                }

                // list必须有size
                if (methodEnum == CacheMethodEnum.getList || methodEnum == CacheMethodEnum.getPageList
                        || methodEnum == CacheMethodEnum.getPageListWithoutSharding) {
                    if (sizeIndex.get(method) == null) {
                        throw new IllegalAccessError("list,pageList必须有size， class:" + this.getClass().getName()
                                + "method: " + method.getName());
                    }
                }

                // pagelist必须有size和cursor
                if (methodEnum == CacheMethodEnum.getPageList
                        || methodEnum == CacheMethodEnum.getPageListWithoutSharding) {
                    if (cursorIndex.get(method) == null) {
                        throw new IllegalAccessError("pagelist,必须有cursor， class:" + this.getClass().getName()
                                + "method: " + method.getName());
                    }
                }

            }
        }

    }

    // ===============================增加=========================================
    /**
     * 插入数据
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    public T insert(T model) throws DataAccessException {
        try {
            // T result=load(model);
            // if(result!=null){
            // return result;
            // }
            model = jdbc_insert(model);

            this.add_object_cache(model);
            this.cache_addOne(model);
            afterInsert(model);
            return model;
        } catch (Exception e) {
            throw new PermissionDeniedDataAccessException("", e);
        }
    }

    // ===============================删除=========================================

    public boolean delete(Object obj) throws DataAccessException {
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

    /**
     * 根据HyperspaceId删除对象
     *
     * @return
     */
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

    /**
     * 根据组合键删除对象
     *
     * @param param
     *            包含组合键的一个对象
     * @param sharding
     *            分表值，如果分表字段是组合键中的其中一个 可以不填
     * @return
     */
    protected boolean delete_combination(T param, long... sharding) throws DataAccessException {

        boolean ret = false;
        T model = this.load_combination(param, true, sharding);
        if (sharding == null || sharding.length == 0) {
            ret = super.jdbc_delete_combination(param);
        } else {
            ret = super.jdbc_delete_combination(param, sharding[0]);
        }
        if (ret && this.enableObjectCache) {
            this.delete_object_cache(model);
        }
        if (ret) {
            cache_deleteOne(model);
        }
        afterDelete(model);
        return ret;
    }

    /**
     * 根据id主键删除对象
     *
     * @param id
     *            包含组合键的一个对象
     * @param sharding
     *            分表值，如果分表字段是主键 可以不填
     * @return
     */
    protected boolean delete_identity(long id, long... sharding) throws DataAccessException {

        boolean ret = false;
        T model = this.load_identity(id, true, sharding);
        if (sharding == null || sharding.length == 0) {
            ret = super.jdbc_delete_identity(id);
        } else {
            ret = super.jdbc_delete_identity(id, sharding[0]);
        }
        if (ret && this.enableObjectCache) {
            this.delete_object_cache(model);
        }
        if (ret) {
            cache_deleteOne(model);
        }
        afterDelete(model);
        return ret;
    }

    // ===============================修改=========================================
    /**
     * 修改
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    public boolean update(T model) throws DataAccessException {
        if (primaryFieldInfo != null && combinationFieldInfos.size() == 0) {
            return update_identity(model);
        }
        if (primaryFieldInfo == null && combinationFieldInfos.size() >= 0) {
            return update_combination(model);
        }
        if (primaryFieldInfo != null && combinationFieldInfos.size() > 0) {
            long id = 0;
            try {
                id = ReflectUtil.getLongValueForLongOrInt(this.primaryFieldInfo.getReadMethod().invoke(model));
            } catch (Exception e) {
                throw new DataAccessResourceFailureException("", e);
            }
            if (id > 0) {
                return update_identity(model);
            }
            return update_combination(model);
        }
        return false;
    }

    /**
     * 根据组合键进行更新操作
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    private boolean update_combination(T model) throws DataAccessException {

        T oldModel = this.load_combination(model, true);
        boolean ret = super.jdbc_update_combination(model);
        try {
            this.handle_ctime(oldModel, model);

            if (ret && this.enableObjectCache) {
                this.handle_combination(model);
                this.delete_object_cache(model);
                this.add_object_cache(model);

            }

            if (ret) {
                this.cache_deleteOne(oldModel);
                this.cache_addOne(model);
            }

        } catch (Exception e) {
            throw new DataAccessResourceFailureException("", e);
        }
        afterUpteDate(oldModel, model);
        return ret;
    }

    /**
     * 根据id主键 进行缓存更新操作
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    private boolean update_identity(T model) throws DataAccessException {
        long id;
        try {
            id = ReflectUtil.getLongValueForLongOrInt(this.primaryFieldInfo.getReadMethod().invoke(model));
        } catch (Exception e1) {
            throw new DataAccessResourceFailureException("", e1);
        }
        T oldModel = this.load_identity(id, true);
        boolean ret = super.jdbc_update_identity(model);
        try {
            this.handle_ctime(oldModel, model);

            if (ret && this.enableObjectCache) {
                this.handle_identity(model);
                this.delete_object_cache(model);
                this.add_object_cache(model);
            }
            if (ret) {
                this.cache_deleteOne(oldModel);
                this.cache_addOne(model);
            }
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("", e);
        }
        afterUpteDate(oldModel, model);
        return ret;
    }

    // ===============================自增=========================================

    /**
     * 自增
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    public T increment(T model, String filedName, int incrementValue) throws DataAccessException {
        if (primaryFieldInfo != null && combinationFieldInfos.size() == 0) {
            return increment_identity(model, filedName, incrementValue);
        }
        if (primaryFieldInfo == null && combinationFieldInfos.size() >= 0) {
            return increment_combination(model, filedName, incrementValue);
        }
        if (primaryFieldInfo != null && combinationFieldInfos.size() > 0) {
            long id = 0;
            try {
                id = ReflectUtil.getLongValueForLongOrInt(this.primaryFieldInfo.getReadMethod().invoke(model));
            } catch (Exception e) {
                throw new DataAccessResourceFailureException("", e);
            }
            if (id > 0) {
                return increment_identity(model, filedName, incrementValue);
            }
            return increment_combination(model, filedName, incrementValue);
        }
        return model;
    }

    /**
     * 根据组合键进行自增操作
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    private T increment_combination(T model, String filedName, int incrementValue) throws DataAccessException {

        T oldModel = this.load_combination(model, true);
        boolean ret = super.jdbc_increment_combination(model, filedName, incrementValue);
        if (!ret) {
            throw new DataAccessResourceFailureException("increment error");
        }
        try {
            this.handle_ctime(oldModel, model);
            // 击穿缓存 获取新数据
            model = load(getHyperspaceIdByEntity(model), false);
            if (ret && this.enableObjectCache) {
                this.handle_combination(model);
                this.delete_object_cache(model);
                this.add_object_cache(model);
            }

            if (ret) {
                this.cache_deleteOne(oldModel);
                this.cache_addOne(model);
            }

        } catch (Exception e) {
            throw new DataAccessResourceFailureException("", e);
        }
        afterIncrement(oldModel, model);
        return model;
    }

    /**
     * 根据id主键 进行缓存自增操作
     *
     * @param model
     * @return
     * @throws DataAccessException
     */
    private T increment_identity(T model, String filedName, int incrementValue) throws DataAccessException {
        long id;
        try {
            id = ReflectUtil.getLongValueForLongOrInt(this.primaryFieldInfo.getReadMethod().invoke(model));

        } catch (Exception e1) {
            throw new DataAccessResourceFailureException("", e1);
        }
        T oldModel = this.load_identity(id, true);
        boolean ret = super.jdbc_increment_identity(model, filedName, incrementValue);
        if (!ret) {
            throw new DataAccessResourceFailureException("increment error");
        }
        try {
            this.handle_ctime(oldModel, model);
            // 击穿缓存 获取新数据
            model = load(getHyperspaceIdByEntity(model), false);
            if (ret && this.enableObjectCache) {
                this.handle_identity(model);
                this.delete_object_cache(model);
                this.add_object_cache(model);
            }
            if (ret) {
                this.cache_deleteOne(oldModel);
                this.cache_addOne(model);
            }
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("", e);
        }
        afterIncrement(oldModel, model);
        return model;
    }

    // ===============================查询=========================================

    public T load(Object obj) throws DataAccessException {
        if (obj instanceof HyperspaceId) {
            return load((HyperspaceId) obj, true);
        } else if (obj instanceof Long) {
            return load_identity((Long) obj, true);
        } else if (obj.getClass().getName().equals(entityClass.getName())) {
            try {
                return load(getHyperspaceIdByEntity((T) obj), true);
            } catch (Exception e) {
                throw new RuntimeException("", e);
            }
        } else {
            throw new RuntimeException("not support:" + obj.getClass() + ",entityClass:" + entityClass);
        }
    }

    /***
     * 批量查询
     *
     * @return
     * @throws DataAccessException
     */
    public List<T> load(List objs) throws Exception {
        List<HyperspaceId> hyperspaceIds = new ArrayList<HyperspaceId>();
        for (Object obj : objs) {
            hyperspaceIds.add(getHyperspaceIdByObj(obj));
        }
        return loadList(hyperspaceIds);
    }

    /***
     * 批量查询
     *
     * @param hyperspaceIds
     * @return
     * @throws DataAccessException
     */
    public List<T> loadList(List<HyperspaceId> hyperspaceIds) throws DataAccessException {
        Map<HyperspaceId, String> id2cacheKeysMap = getCacheKeys(hyperspaceIds);
        List<String> keys = new ArrayList<String>();
        for (HyperspaceId id : id2cacheKeysMap.keySet()) {
            String key = id2cacheKeysMap.get(id);
            keys.add(key);
        }
        Map<String, Object> cacheValues = JedisXUtils.multiGetObject(cache, keys, null);
        List<T> result = new ArrayList<T>();

        if (hyperspaceIds != null) {
            for (HyperspaceId hyperspaceId : hyperspaceIds) {
                T value = (T) cacheValues.get(id2cacheKeysMap.get(hyperspaceId));

                if (value == null)
                    value = load(hyperspaceId, true);

                if (value != null)
                    result.add(value);
            }
        }

        return result;
    }

    private Map<HyperspaceId, String> getCacheKeys(List<HyperspaceId> hyperspaceIds) {
        Map<HyperspaceId, String> result = new LinkedHashMap<HyperspaceId, String>();
        for (HyperspaceId hyperspaceId : hyperspaceIds) {
            String cacheKey = null;
            if (hyperspaceId instanceof HyperspacePrimaryId) {
                HyperspacePrimaryId hyperspacePrimaryId = (HyperspacePrimaryId) hyperspaceId;
                cacheKey = this.get_identity_object_cacheKey(hyperspacePrimaryId.getId());
            } else {
                try {
                    HyperspaceCombinationId hyperspaceCombinationId = (HyperspaceCombinationId) hyperspaceId;
                    cacheKey = this
                            .get_combination_object_cacheKey(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId));
                } catch (Exception e) {
                    throw new PermissionDeniedDataAccessException("", e);
                }

            }
            result.put(hyperspaceId, cacheKey);
        }
        return result;
    }

    /**
     * 根据HyperspaceId查询对象
     *
     * @return
     */
    protected T load(HyperspaceId hyperspaceId, boolean cacheLoad) throws DataAccessException {
        if (hyperspaceId instanceof HyperspacePrimaryId) {
            HyperspacePrimaryId hyperspacePrimaryId = (HyperspacePrimaryId) hyperspaceId;
            return load_identity(hyperspacePrimaryId.getId(), cacheLoad, hyperspacePrimaryId.getSharding());
        } else {
            try {
                HyperspaceCombinationId hyperspaceCombinationId = (HyperspaceCombinationId) hyperspaceId;
                if (hyperspaceCombinationId.getSharding() == 0) {
                    return load_combination(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId),
                            cacheLoad);
                } else {
                    return load_combination(createParamModelByHyperspaceCombinationId(hyperspaceCombinationId),
                            cacheLoad, hyperspaceCombinationId.getSharding());
                }
            } catch (Exception e) {
                throw new PermissionDeniedDataAccessException("", e);
            }

        }
    }

    /**
     * 根据id主键load对象
     *
     * @param id
     *            包含组合键的一个对象
     * @param sharding
     *            分表值，如果分表字段是主键 可以不填
     * @return
     */
    protected T load_identity(long id, boolean cacheLoad, long... sharding) {

        if (isShardingTable && shardingFieldInfo != primaryFieldInfo && (sharding == null || sharding.length == 0)) {
            throw new IllegalAccessError("找不到分表字段");
        }

        T model = null;
        String cacheKey = this.get_identity_object_cacheKey(id);
        if (cacheLoad && this.enableObjectCache) {
            model = this.hit_object_cache(cacheKey);
        }
        if (model != null) {
            return model;
        }
        if (sharding == null || sharding.length == 0) {
            model = jdbc_load_identity(id);
        } else {
            model = jdbc_load_identity(id, sharding[0]);
        }
        if (model != null && this.enableObjectCache && cacheLoad) {
            this.add_object_cache(model);
        }
        return model;
    }

    /**
     * 根据组合键load出对象
     *
     * @param param
     *            包含组合键的一个对象
     * @param cacheLoad
     *            是否从缓存load
     * @param sharding
     *            分表值，如果分表字段是组合键中的其中一个 可以不填
     * @return
     */
    protected T load_combination(T param, boolean cacheLoad, long... sharding) {
        if (isShardingTable && !combinationFieldInfos.contains(shardingFieldInfo)
                && (sharding == null || sharding.length == 0)) {
            throw new IllegalAccessError("找不到分表字段");
        }

        T model = null;
        String cacheKey = this.get_combination_object_cacheKey(param);
        if (cacheLoad && this.enableObjectCache) {
            model = this.hit_object_cache(cacheKey);
        }
        if (model != null) {
            return model;
        }
        if (sharding == null || sharding.length == 0) {
            model = super.jdbc_load_combination(param);
        } else {
            model = super.jdbc_load_combination(param, sharding[0]);
        }
        if (model != null && this.enableObjectCache && cacheLoad) {
            this.add_object_cache(model);
        }
        return model;
    }

    // ===============================list和count=========================================

    public Long cache_getRank(String unDecidedCacheKey, CacheSortOrder order, long shardingId,
            String sql, Object item, Object[] sqlParams) throws DataAccessException {
    	Long res = null;
    	String decidedCacheKey = decideListCacheKey(unDecidedCacheKey, order);
    	HyperspaceId id = null;
		try {
			id = this.getHyperspaceIdByObj(item);
		} catch (Exception e) {
			throw new IllegalArgumentException("item is error");
		}
    	if(JedisXUtils.exists(cache, decidedCacheKey)){
    		if (order == CacheSortOrder.asc) {
    			res = JedisXUtils.zrankObject(cache, decidedCacheKey, id);
    		} else {
    			res = JedisXUtils.zrevrankObject(cache, decidedCacheKey, id);
    		}
    	} else {
    		if(!JedisXUtils.exists(cache, decidedCacheKey) &&
        			!JedisXUtils.exists(cache, decideIsNullListCacheKey(decidedCacheKey))){
        		ZkLock lock = null;
        		try {
        			lock = ZkLock.getAndLock(hyperspaceConfig.getZkAddress(), "hyperSpace", decidedCacheKey, true,
                            30000, 30000);
        			if(!JedisXUtils.exists(cache, decidedCacheKey) &&
        	    			!JedisXUtils.exists(cache, decideIsNullListCacheKey(decidedCacheKey))) {
        				List<T> temp = super.jdbc_getList(shardingId, sql, sqlParams);
                        if (temp != null && temp.size() > 0) {
                            _addListCache(temp, decidedCacheKey);
                        } else {
                            logger.info(daoClassName + "@@@=====@@@setListNull on cache_getRank:"
                                        + decideIsNullListCacheKey(decidedCacheKey));
                            JedisXUtils.setString(cache, decideIsNullListCacheKey(decidedCacheKey), DEFAULT_EXPIRED_TIME, "true", false);
                        }
        			}
    			} catch (Exception e) {
    				throw new PermissionDeniedDataAccessException("", e);
                } finally {
                    if (lock != null)
                        lock.release();
                }
        		if(JedisXUtils.exists(cache, decidedCacheKey)){
        			if (order == CacheSortOrder.asc) {
            			res = JedisXUtils.zrankObject(cache, decidedCacheKey, id);
            		} else {
            			res = JedisXUtils.zrevrankObject(cache, decidedCacheKey, id);
            		}
        		}
        	}
    	}
    	return res;
    }

    public List<HyperspaceId> cache_getAllList(String unDecidedCacheKey, CacheSortOrder order, long shardingId,
            String sql, Object[] sqlParams) throws DataAccessException {
        String decidedCacheKey = decideListCacheKey(unDecidedCacheKey, order);
        List<HyperspaceId> result = new ArrayList<HyperspaceId>();
        Set<Object> set = null;
        if (order == CacheSortOrder.asc) {
            set = JedisXUtils.zrangeByScoreObject(cache, decidedCacheKey, 0, Double.MAX_VALUE, null);
        } else {
            set = JedisXUtils.zrevrangeByScoreObject(cache, decidedCacheKey, Double.MAX_VALUE, 0, null);
        }
        if (set != null && set.size() > 0) {
            logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@hit_cache:" + decidedCacheKey);
            for (Object one : set) {
                result.add((HyperspaceId) one);
            }
        } else {
            if (!JedisXUtils.exists(cache, decideIsNullListCacheKey(decidedCacheKey))) {
                List<T> temp = super.jdbc_getList(shardingId, sql, sqlParams);
                if (temp != null && temp.size() > 0) {
                    logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@add_cache:" + decidedCacheKey);
                    try {
                        result = this._addListCache(temp, decidedCacheKey);
                    } catch (Exception e) {
                        throw new PermissionDeniedDataAccessException("", e);
                    }
                } else {
                    logger.info(daoClassName + "@@@=====@@@setListNull on cache_getAllList:"
                            + decideIsNullListCacheKey(decidedCacheKey));
                    JedisXUtils.setString(cache, decideIsNullListCacheKey(decidedCacheKey), DEFAULT_EXPIRED_TIME, "true", false);
                }
            }
        }
        return result;
    }

    public List<HyperspaceId> cache_getPageList(String unDecidedCacheKey, CacheSortOrder order, long shardingId,
            String sql, String pageSql, double cursor, int size, Object[] sqlParams, Object[] pageParams)
            throws DataAccessException {
        // 1.从redis查找数据
        // 2.如果没有数据 判断是否已经初始化，如果没有初始化（缓存在不在） 就初始化一次，然后再从redis取出数据
        // 3.如果还没有数据 就改用sql查询
        String decidedCacheKey = decideListCacheKey(unDecidedCacheKey, order);
        List<HyperspaceId> result = new ArrayList<HyperspaceId>();
        double minScore = 0, maxScore = 0;
        Set<Object> set = null;
        if (order == CacheSortOrder.asc) {
            // minScore = cursor + 0.1;
            // maxScore = Long.MAX_VALUE;
            minScore = cursor;
            maxScore = Double.MAX_VALUE;
            set = JedisXUtils.zrangeByScoreObject(cache, decidedCacheKey, minScore, maxScore, 0, size, null);
        } else {
            // cursor = (cursor == 0) ? Long.MAX_VALUE : cursor;
            // maxScore = cursor - 0.1;
            maxScore = (cursor == 0) ? Double.MAX_VALUE : cursor;
            set = JedisXUtils.zrevrangeByScoreObject(cache, decidedCacheKey, maxScore, minScore, 0, size, null);
        }

        // 初始化list
        if ((null == set || set.size() == 0) && !cache.exists(decidedCacheKey)) {
            if (!JedisXUtils.exists(cache, decideIsNullListCacheKey(decidedCacheKey))) {
                ZkLock lock = null;
                try {
                    lock = ZkLock.getAndLock(hyperspaceConfig.getZkAddress(), "hyperSpace", decidedCacheKey, true,
                            30000, 30000);
                    // 如果此时 某人增加数据，有可能存入数据库成功，但是此项操作加载数据库时候没加载上来。
                    if (!JedisXUtils.exists(cache, decidedCacheKey)) {
                        List<T> temp = super.jdbc_getList(shardingId, sql, sqlParams);
                        if (temp != null && temp.size() > 0) {
                            _addListCache(temp, decidedCacheKey);
                        } else {
                            if (cursor == 0) {
                                logger.info(daoClassName + "@@@=====@@@setListNull on cache_getPageList:"
                                        + decideIsNullListCacheKey(decidedCacheKey));
                                JedisXUtils.setString(cache, decideIsNullListCacheKey(decidedCacheKey), DEFAULT_EXPIRED_TIME, "true", false);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new PermissionDeniedDataAccessException("", e);
                } finally {
                    if (lock != null)
                        lock.release();
                }

                if (order == CacheSortOrder.asc) {
                    set = JedisXUtils.zrangeByScoreObject(cache, decidedCacheKey, minScore, maxScore, 0, size, null);
                } else {
                    set = JedisXUtils.zrevrangeByScoreObject(cache, decidedCacheKey, maxScore, minScore, 0, size, null);
                }
            }
        }

        if (set != null && set.size() > 0) {
            logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@hit_cache:" + decidedCacheKey);
            for (Object obj : set) {
                result.add((HyperspaceId) obj);
            }
        } else {
            // 如果 存在list(可以查询)
            // 如果不存在null标识(可以查询)。
            if (!JedisXUtils.exists(cache, decideIsNullListCacheKey(decidedCacheKey)) || JedisXUtils.exists(cache, decidedCacheKey)) {
                List<HyperspaceAllId> list = this.__getPageList(shardingId, pageSql, pageParams);
                if (list != null && list.size() > 0) {
                    for (HyperspaceAllId allId : list) {
                        if (allId.getHyperspacePrimaryId() != null) {
                            result.add(allId.getHyperspacePrimaryId());
                        } else {
                            result.add(allId.getHyperspaceCombinationId());
                        }
                    }
                } else {
                    // cache.setString(decideIsNullListCacheKey(decidedCacheKey),
                    // DEFAULT_EXPIRED_TIME, "true");
                }
            }
        }

        return result;
    }

    public List<HyperspaceId> cache_getMergeList(List<String> cacheKeys, int size, CacheSortOrder order)
            throws DataAccessException {

        Set<Object> set = null;
        if (order == CacheSortOrder.asc) {
            set = JedisXUtils.zunionstoreRangeByScoreObject(cache, cacheKeys, Double.MIN_VALUE, Double.MAX_VALUE, 0, size, null);
        } else {
            set = JedisXUtils.zunionstoreRevrangeByScoreObject(cache, cacheKeys, Double.MAX_VALUE, Double.MIN_VALUE, 0, size, null);
        }

        List<HyperspaceId> hyperspaceIds = new ArrayList<HyperspaceId>();
        if (set != null)
            for (Object obj : set) {
                hyperspaceIds.add((HyperspaceId) obj);
            }
        return hyperspaceIds;
    }

    //
    public List<HyperspaceId> cache_getList(String unDecidedCacheKey, String countCacheKey, CacheSortOrder order,
            long shardingId, String sql, String countSql, int size, Object[] args, Object[] countArgs, int i)
            throws DataAccessException {
        String decidedCacheKey = decideListCacheKey(unDecidedCacheKey, order);
        List<HyperspaceId> result = new ArrayList<HyperspaceId>();
        int end = size;
        if (size > 0) {
            end--;
        }
        Set<Object> set = null;
        if (order == CacheSortOrder.asc) {
            set = JedisXUtils.zrangeObject(cache, decidedCacheKey, 0, end, null);
        } else {
            set = JedisXUtils.zrevrangeObject(cache, decidedCacheKey, 0, end, null);
        }
        // 初始化list
        if ((null == set || set.size() == 0) && !cache.exists(decidedCacheKey)) {
            if (!JedisXUtils.exists(cache, decideIsNullListCacheKey(decidedCacheKey))) {
                ZkLock lock = null;
                try {
                    lock = ZkLock.getAndLock(hyperspaceConfig.getZkAddress(), "hyperSpace", decidedCacheKey, true,
                            30000, 30000);

                    // 如果此时 某人增加数据，有可能存入数据库成功，但是此项操作加载数据库时候没加载上来。
                    if (!JedisXUtils.exists(cache, decidedCacheKey)) {
                        args[args.length - 1] = DEFAULT_INIT_LIST_SIZE;
                        List<T> temp = super.jdbc_getList(shardingId, sql, args);
                        if (temp != null && temp.size() > 0) {
                            _addListCache(temp, decidedCacheKey);
                        } else {
                            JedisXUtils.setString(cache, decideIsNullListCacheKey(decidedCacheKey), DEFAULT_EXPIRED_TIME, "true", false);
                        }
                    }
                } catch (Exception e) {
                    throw new PermissionDeniedDataAccessException("", e);
                } finally {
                    if (lock != null)
                        lock.release();
                }
                if (order == CacheSortOrder.asc) {
                    set = JedisXUtils.zrangeObject(cache, decidedCacheKey, 0, end, null);
                } else {
                    set = JedisXUtils.zrevrangeObject(cache, decidedCacheKey, 0, end, null);
                }
            }
        }
        if (null != set && set.size() > 0) {
            logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@hit_cache:" + decidedCacheKey);
            for (Object one : set) {
                result.add((HyperspaceId) one);
            }
        }
        return result;
    }

    private List<HyperspaceAllId> __getPageList(long shardingId, String pageSql, Object[] pageParams)
            throws DataAccessException {
        try {

            List<HyperspaceAllId> list = this.getJdbcTemplate(shardingId, true).getJdbcTemplate().query(pageSql,
                    new HyperspaceAllIdRowMapper(this), pageParams);

            logger.info(daoClassName + "get page sql:{},pageParams:{}", pageSql, pageParams);
            if (null != list) {
                return list;
            } else {
                return new ArrayList<HyperspaceAllId>();
            }
        } catch (DataAccessException e) {
            logger.error(daoClassName, e);
            return new ArrayList<HyperspaceAllId>();
        }

    }

//    private void initListCache(List<T> list, String decidedCacheKey) throws DataAccessException {
//        ZkLock lock = null;
//        try {
//            lock = ZkLock
//                    .getAndLock(hyperspaceConfig.getZkAddress(), "hyperSpace", decidedCacheKey, true, 30000, 30000);
//
//            cache.delete(decidedCacheKey);
//            _addListCache(list, decidedCacheKey);
//
//        } catch (Exception e) {
//            throw new PermissionDeniedDataAccessException("", e);
//        } finally {
//            if (lock != null)
//                lock.release();
//        }
//    }

    /**
     * 进行缓存
     *
     * @param list
     * @return 有主键 优先返回主键,没有主键返回组合键
     * @throws Exception
     */
    private List<HyperspaceId> _addListCache(List<T> list, String decidedCacheKey) throws Exception {
        List<HyperspaceId> result = new ArrayList<HyperspaceId>();
        if (list != null && list.size() > 0) {
           // Map<Double, Object> members = new HashMap<Double, Object>();
        	Map<Object, Double> members = new HashMap<Object, Double>();
            for (T one : list) {

                Double cache_sort = 0d;
                try {

                    cache_sort = Double.valueOf((sortFieldInfo.getReadMethod().invoke(one)).toString());
                } catch (Exception e) {
                    throw new PermissionDeniedDataAccessException("", e);
                }
                HyperspaceId hyperspaceId = getHyperspaceIdByEntity(one);
                result.add(hyperspaceId);
                members.put(hyperspaceId, cache_sort);
//                if (members.keySet().contains(cache_sort)) {
//                    this.cache.zaddObject(decidedCacheKey, cache_sort, hyperspaceId);
//                } else {
//                    members.put(cache_sort, hyperspaceId);
//                }
            }
            JedisXUtils.zaddMultiObject(cache, decidedCacheKey, members, false);
            JedisXUtils.expire(cache, decidedCacheKey, expireSeconds, false);
        }
        return result;
    }

    public int cache_getCount(String decidedCacheKey, long shardingId, String sql, Object[] args)
            throws DataAccessException {
        int count = 0;
        String obj = JedisXUtils.getString(cache, decidedCacheKey);
        if (obj != null) {
            logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@hit_cache:" + decidedCacheKey);

            count = Integer.parseInt(obj);
        } else {
            count = this.jdbc_getCount(shardingId, sql, args);
            logger.info(daoClassName + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@add_cache:" + decidedCacheKey);

            JedisXUtils.setString(cache, decidedCacheKey, this.expireSeconds, String.valueOf(count), false);
        }
        return count;
    }

    /**
     * 根据组合键 把对象从缓存中load出来,把主键值赋值。
     *
     * @param model
     * @throws Exception
     */
    private void handle_combination(T model) throws Exception {
        if (primaryFieldInfo != null) {
            T old = this.load_combination(model, true);
            Object id = this.primaryFieldInfo.getReadMethod().invoke(old);
            this.primaryFieldInfo.getWriteMethod().invoke(model, id);
        }
    }

    /**
     * 根据ID主键 对象从缓存中load出来,把组合键值赋值。
     *
     * @param model
     * @throws Exception
     */
    private void handle_identity(T model) throws Exception {
        if (combinationFieldInfos.size() > 0) {
            long id = ReflectUtil.getLongValueForLongOrInt(this.primaryFieldInfo.getReadMethod().invoke(model));
            T old = this.load_identity(id, true);
            for (FieldInfo fieldInfo : combinationFieldInfos) {
                Object value = fieldInfo.getReadMethod().invoke(old);
                fieldInfo.getWriteMethod().invoke(model, value);
            }
        }
    }

    private void handle_ctime(T old, T target) {
        try {
            if (null != ctimeFieldInfo) {
                Object value = ctimeFieldInfo.getReadMethod().invoke(old);
                ctimeFieldInfo.getWriteMethod().invoke(target, value);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 判断是否击中缓存
     *
     * @return
     */
    protected T hit_object_cache(String decidedCacheKey) {
        T model = (T) JedisXUtils.getObject(cache, decidedCacheKey, null);
        // if (model != null) {
        // logger.info(daoClassName + "cache hitting :" + model);
        // }
        return model;
    }

    /**
     * 获取组合键的缓存key
     *
     * @param param
     * @return
     */
    protected String get_combination_object_cacheKey(T param) {
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

    /**
     * 根据id获取缓存key
     *
     * @param id
     * @return
     */
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

    /**
     * 根据对象 获取基于主键的对象缓存key
     *
     * @param param
     * @return
     */
    protected String get_identity_object_cacheKey(T param) {
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

    /**
     * 删除对象缓存
     *
     * @param model
     */
    protected void delete_object_cache(T model) {
        String cacheKey = null;
        if (primaryFieldInfo != null) {
            cacheKey = this.get_identity_object_cacheKey(model);
            if (JedisXUtils.exists(cache, cacheKey)) {
                logger.info(daoClassName + "delete identity cache :" + model);
                JedisXUtils.delete(cache, cacheKey);
            }
        }
        if (combinationFieldInfos.size() > 0) {
            cacheKey = this.get_combination_object_cacheKey(model);
            if (JedisXUtils.exists(cache, cacheKey)) {
                logger.info(daoClassName + "delete combination cache :" + model);
                JedisXUtils.delete(cache, cacheKey);
            }
        }
    }

    /***
     * 增加对象缓存
     *
     * @param model
     */
    protected void add_object_cache(T model) {
        String cacheKey = null;
        if (primaryFieldInfo != null) {
            cacheKey = this.get_identity_object_cacheKey(model);
            if (!JedisXUtils.exists(cache, cacheKey)) {
                logger.info(daoClassName + "add identity key:{}, cache :{}", cacheKey, model);
                JedisXUtils.setObject(cache, cacheKey, expireSeconds, model);
            }
        }
        if (combinationFieldInfos.size() > 0) {
            cacheKey = this.get_combination_object_cacheKey(model);
            if (!JedisXUtils.exists(cache, cacheKey)) {
                logger.info(daoClassName + "add combination key:{}, cache :{}", cacheKey, model);
                JedisXUtils.setObject(cache, cacheKey, expireSeconds, model);
            }
        }
    }

    // ////////////////////////////////////////////////////////

    public void cache_decrCount(String countCacheKey) {
        this.handleCount(countCacheKey, -1);
    }

    public void cache_incrCount(String countCacheKey) {
        this.handleCount(countCacheKey, 1);
    }

    private void handleCount(String countCacheKey, long step) {

        String lockKey = countCacheKey + "_key";
        ZkLock lock = null;
        try {
            lock = ZkLock.getAndLock(hyperspaceConfig.getZkAddress(), "hyperSpace", lockKey, true, 30000, 30000);

            if (JedisXUtils.exists(cache, countCacheKey)) {
                if (step > 0) {
                    logger.info(daoClassName + "count incy cache :{},step:{}", new Object[] { countCacheKey, step });
                    JedisXUtils.incrBy(cache, countCacheKey, step);
                } else {
                    logger.info(daoClassName + "count decy cache :{},step:{}", new Object[] { countCacheKey, step });
                    JedisXUtils.decrBy(cache, countCacheKey, Math.abs(step));
                }
                JedisXUtils.expire(cache, countCacheKey, this.expireSeconds, true);
            }
        } catch (Exception e) {
            this.logger.info(daoClassName + e.getMessage());
        } finally {
            if (lock != null)
                lock.release();
        }
    }

    /**
     * 获取真实的cacheKey
     *
     * @param method
     *            哪个方法
     * @param args
     *            这个方法对应的参数
     * @param order
     *            排序 一般现在的方法如果不写，默认是倒序。
     * @return
     */
    public String getCacheKey(Method method, Object[] args, CacheSortOrder order) {
        String unformateCachekey = unformateCacheKeys.get(method);
        String cacheKey = formatCacheKey(unformateCachekey, method, args);
        return decideListCacheKey(cacheKey, order);
    }

    /**
     * 获取判断是否有空值的key
     *
     * @param cachekey
     * @return
     */
    public String getIsNullListCacheKey(String cachekey) {
        return decideIsNullListCacheKey(cachekey);
    }

    private String decideIsNullListCacheKey(String decidedCacheKey) {
        return decidedCacheKey + "_isNull";
    }

    private String formatCacheKey(String cacheKey, Method method, Object[] args) {
        Map<Integer, String> params = getParamsIndexMap().get(method);
        if (params != null) {
            for (int i : params.keySet()) {
                cacheKey = cacheKey.replaceAll(generateCacheKey(params.get(i)),
                        replaceCacheKeyValue(params.get(i), args[i]));
            }
        }
        return cacheKey;
    }

    public String decideListCacheKey(String cacheKey, CacheSortOrder order) {

        if (primaryFieldInfo != null) {
            if (order == CacheSortOrder.asc) {
                return cacheKey + "_order_asc";
            } else {
                return cacheKey;
            }
        } else {
            if (order == CacheSortOrder.asc) {
                return cacheKey + "_combination_order_asc";
            } else {
                return cacheKey + "_combination";
            }
        }
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

    public Void cache_addOne(T model) {
        String listKey = getHyperspaceConfig().getProjectName() + "_" + entityClass.getName() + listCacheKeysign;
        String countKey = getHyperspaceConfig().getProjectName() + "_" + entityClass.getName() + countCacheKeysign;
        for (String cacheKey : unformateCacheKeys.values()) {
            cacheKey = formartCacheKey(model, cacheKey);
            if (cacheKey.startsWith(listKey)) {
                cache_addOne(decideListCacheKey(cacheKey, CacheSortOrder.desc), null, model);
                cache_addOne(decideListCacheKey(cacheKey, CacheSortOrder.asc), null, model);
            } else if (cacheKey.startsWith(countKey)) {
                cache_addOne(null, cacheKey, model);
            }
        }
        return null;
    }

    public Void cache_deleteOne(T model) {
        String listKey = getHyperspaceConfig().getProjectName() + "_" + entityClass.getName() + listCacheKeysign;
        String countKey = getHyperspaceConfig().getProjectName() + "_" + entityClass.getName() + countCacheKeysign;
        for (String cacheKey : unformateCacheKeys.values()) {
            cacheKey = formartCacheKey(model, cacheKey);
            if (cacheKey.startsWith(listKey)) {
                cache_deleteOne(decideListCacheKey(cacheKey, CacheSortOrder.desc), null, model);
                cache_deleteOne(decideListCacheKey(cacheKey, CacheSortOrder.asc), null, model);
            } else if (cacheKey.startsWith(countKey)) {
                cache_deleteOne(null, cacheKey, model);
            }
        }
        return null;

    }

    public Void cache_addOne(String listCacheKey, String countCacheKey, T model) {
        try {
            if (listCacheKey != null && JedisXUtils.exists(cache, listCacheKey)) {
                double cacheSort = Double.valueOf(sortFieldInfo.getReadMethod().invoke(model).toString());
                logger.info(daoClassName + "cache_addOne cache :{},:{},model:{}", new Object[] { listCacheKey, cache,
                        model });
                JedisXUtils.zaddObject(cache, listCacheKey, cacheSort, getHyperspaceIdByEntity(model));
                long count = JedisXUtils.zcard(cache, listCacheKey);
                if (count > DEFAULT_MAX_LIST_SIZE) {
                    if (onlyHasDescList) {
                        // 这种移除方法，只适合倒序的list
                        // 假如数据是从1到1100
                        // 倒序list: 1096,1097,1098,1099,1100 ,取的时候是倒序取的
                        // 正序list: 1,2,3,4,5 ,取的时候是正序取的
                        int num = (int) count - (DEFAULT_MAX_LIST_SIZE) - 1;
                        JedisXUtils.zremrangeByRank(cache, listCacheKey, 0, num);
                    } else {
                        JedisXUtils.delete(cache, listCacheKey);
                    }

                }
            } else {
                JedisXUtils.delete(cache, decideIsNullListCacheKey(listCacheKey));
            }
            if (countCacheKey != null) {
                this.cache_incrCount(countCacheKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        return null;
    }

    public Void cache_deleteOne(String listCacheKey, String countCacheKey, T model) {
        try {
            if (listCacheKey != null && JedisXUtils.exists(cache, listCacheKey)) {
                logger.info(daoClassName + "cache_deleteOne cache :{},model:{}", listCacheKey, model);
                JedisXUtils.zremObject(cache, listCacheKey, getHyperspaceIdByEntity(model));
            }
            if (countCacheKey != null) {
                this.cache_decrCount(countCacheKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
        return null;
    }

    public HyperspaceId getHyperspaceIdByObj(Object obj) throws Exception {
        if (obj instanceof HyperspaceId) {
            return (HyperspaceId) obj;
        } else if (obj instanceof Long) {
            if (primaryFieldInfo == null || primaryFieldInfo != shardingFieldInfo) {
                throw new IllegalAccessError("此结构必须传入shardingId或者包含组合键的对象:" + obj.getClass() + ",entityClass:"
                        + entityClass);
            }
            long id = (Long) obj;
            return new HyperspacePrimaryId(id, id);
        } else if (obj.getClass().getName().equals(entityClass.getName())) {
            return getHyperspaceIdByEntity((T) obj);
        } else {
            throw new RuntimeException("not support:" + obj.getClass() + ",entityClass:" + entityClass);
        }
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

    public void setExpireSeconds() {
    }

    public static void main(String[] sdg) {

        Map<Double, Object> members = new HashMap<Double, Object>();
        members.put(new Double(1), "1");
        System.out.println(members.keySet().contains(new Double(1)));
        System.out.println(members.keySet().contains(1d));

    }

}
