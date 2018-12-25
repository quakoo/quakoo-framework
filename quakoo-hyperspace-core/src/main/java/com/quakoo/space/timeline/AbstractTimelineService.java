package com.quakoo.space.timeline;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.quakoo.space.annotation.timeline.TimelineMethod;
import com.quakoo.space.annotation.timeline.TimelineMethodParam;
import com.quakoo.space.annotation.timeline.TimelineRepetFields;
import com.quakoo.space.annotation.timeline.TimelineSort;
import com.quakoo.space.enums.timeline.TimelineMethodEnum;
import com.quakoo.space.enums.timeline.TimelineMethodParamEnum;
import com.quakoo.space.model.timeline.TimelineSession;
import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.PermissionDeniedDataAccessException;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.space.HyperspaceConfig;
import com.quakoo.space.annotation.timeline.TimelineMethod;
import com.quakoo.space.annotation.timeline.TimelineMethodParam;
import com.quakoo.space.annotation.timeline.TimelineRepetFields;
import com.quakoo.space.annotation.timeline.TimelineSort;
import com.quakoo.space.enums.timeline.TimelineMethodEnum;
import com.quakoo.space.enums.timeline.TimelineMethodParamEnum;
import com.quakoo.space.model.timeline.TimelineSession;

/**
 * 基础的timelineService
 *
 * @author yongbiaoli
 *
 * @param <T>
 */
public abstract class AbstractTimelineService<T> implements InitializingBean,
		TimelineService<T> {

	Logger logger = LoggerFactory.getLogger(AbstractTimelineService.class);

	protected double DEFAULT_RATE = 1.5;// 过滤因子。如果需要动态过滤，则默认每次多取多少个数据
	protected int DEFAULT_MAX_LIST_SIZE = 20000;// 最大的list的值，超过会清除重新聚合
	protected int DEFAULT_INIT_LIST_SIZE = 3000;// 初始最小的list的值
	protected int DEFAULT_EXPIRED_TIME = 60 * 60 * 24 * 10;// 缓存时间10天

	/**
	 * 缓存
	 */
	@Autowired(required = false)
	@Qualifier("cachePool")
	protected JedisX cache;

	@Autowired(required = true)
	@Qualifier("hyperspaceConfig")
	protected HyperspaceConfig hyperspaceConfig;

	// 未被格式化的CacheKeys。此key用来存结果
	// method :timelineService的method
	// String:未被格式化的key
	private Map<Method, String> unformateCacheKeys = new HashMap<Method, String>();

	// 未被格式化的去重key.此key用来存去重的结果
	// method :timelineService的method
	// Map<Integer,String>:实体类上标注的group值-未被格式化的去重key
	private Map<Method, Map<Integer, String>> unformateGroupRepetCacheKeys = new HashMap<Method, Map<Integer, String>>();

	// 未被格式化的去重key和它对应的list方法（entity的只读方法）
	// String :未被格式化的去重key
	// List<Method>:entity的只读方法
	private Map<Method, Map<String, List<Method>>> unformateGroupRepetCacheKeyMethod = new HashMap<Method, Map<String, List<Method>>>();

	/**
	 * 实体类
	 */
	private Class entityClass = null;

	/**
	 * 实体类的排序字段
	 */
	private Method sortMethod = null;

	/**
	 * fieldName-readMethod(实体类的属性和对应的读方法)
	 */
	private Map<String, Method> filedsMap = new HashMap<String, Method>();

	/**
	 * groupId-readMethod[] (组id和他们对应的读方法)
	 */
	private Map<Integer, List<Method>> group_methodMap = new HashMap<Integer, List<Method>>();

	private Map<Method, Integer> sizeIndex = new HashMap<Method, Integer>();

	private Map<Method, Integer> cursorIndex = new HashMap<Method, Integer>();

	private Map<Method, Integer> sessionIndex = new HashMap<Method, Integer>();

	/**
	 * method:方法 Map<Integer, String> ，integer:方法参数的序号，String：方法参数的名字
	 */
	private Map<Method, Map<Integer, String>> paramsIndexMap = new HashMap<Method, Map<Integer, String>>();

	public Map<Method, String> getUnformateCacheKeys() {
		return unformateCacheKeys;
	}

	public void setUnformateCacheKeys(Map<Method, String> unformateCacheKeys) {
		this.unformateCacheKeys = unformateCacheKeys;
	}

	public Map<Method, Map<Integer, String>> getUnformateGroupRepetCacheKeys() {
		return unformateGroupRepetCacheKeys;
	}

	public void setUnformateGroupRepetCacheKeys(
			Map<Method, Map<Integer, String>> unformateGroupRepetCacheKeys) {
		this.unformateGroupRepetCacheKeys = unformateGroupRepetCacheKeys;
	}

	public Map<Method, Map<String, List<Method>>> getUnformateGroupRepetCacheKeyMethod() {
		return unformateGroupRepetCacheKeyMethod;
	}

	public void setUnformateGroupRepetCacheKeyMethod(
			Map<Method, Map<String, List<Method>>> unformateGroupRepetCacheKeyMethod) {
		this.unformateGroupRepetCacheKeyMethod = unformateGroupRepetCacheKeyMethod;
	}

	public Map<Method, Map<Integer, String>> getParamsIndexMap() {
		return paramsIndexMap;
	}

	public void setParamsIndexMap(
			Map<Method, Map<Integer, String>> paramsIndexMap) {
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

	// =======================初始化============================

	public Map<Method, Integer> getSessionIndex() {
		return sessionIndex;
	}

	public void setSessionIndex(Map<Method, Integer> sessionIndex) {
		this.sessionIndex = sessionIndex;
	}

	protected String getProjectName() {
		return hyperspaceConfig.getProjectName();
	}

	protected String getZookeeperAddress() {
		return hyperspaceConfig.getZkAddress();
	}

	protected String createRepetCacheKey(int group, List<Method> methods) {
		String methodName = "";
		for (Method method : methods) {
			methodName = methodName + method.getName() + "_";
		}
		return getProjectName() + this.getClass().getName()
				+ "_timeline_repetCacheKey" + "_group_" + group
				+ "_methodName_" + methodName + "_";
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		entityClass = ReflectUtil.getGenericType(this.getClass(), 0);
		if (entityClass == null) {
			throw new IllegalClassException("EntityClass is error");
		}

		PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(
				entityClass).getPropertyDescriptors();
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					TimelineSort timeLineSort = field
							.getAnnotation(TimelineSort.class);
					TimelineRepetFields repetByEquals = field
							.getAnnotation(TimelineRepetFields.class);
					if (timeLineSort != null) {
						sortMethod = one.getReadMethod();
					}
					if (repetByEquals != null) {
						int group = repetByEquals.group();
						List<Method> groupMethods = group_methodMap.get(group);
						if (groupMethods == null) {
							groupMethods = new ArrayList<Method>();
							group_methodMap.put(group, groupMethods);
						}
						groupMethods.add(one.getReadMethod());
						group_methodMap.put(group, groupMethods);
					}
					filedsMap.put(fieldName, one.getReadMethod());
				}
			}
		}

		// timelineMethod 和对应的 未格式化的cachekey
		// timelineMethod 和对应的 未格式化的去重组的cachekey（多个）

		// 生成 cacheKey

		Method[] methods = this.getClass().getMethods();
		for (Method method : methods) {
			TimelineMethod timelineMethod = method
					.getAnnotation(TimelineMethod.class);
			if (null != timelineMethod
					&& timelineMethod.methodEnum() == TimelineMethodEnum.getPageList) {
				String cacheKey = getProjectName() + entityClass.getName()
						+ "_timeline_" + method.getName() + "_";
				LinkedHashMap<Integer, String> fieldNames = getParamsFields(method);
				for (int i : fieldNames.keySet()) {
					String fieldName = fieldNames.get(i);
					cacheKey = cacheKey + generateCacheKey(fieldName);
				}
				unformateCacheKeys.put(method, cacheKey);
				logger.info(entityClass
						+ "class:{},method:{},unformateCacheKey:{}",
						new Object[] { entityClass.getName(), method.getName(),
								cacheKey });

				System.out.println("cacheKey:" + cacheKey);

				Map<Integer, String> groupRepetCacheKeys = new HashMap<Integer, String>();
				Map<String, List<Method>> cacheKey_methodMap = new HashMap<String, List<Method>>();
				for (Integer group : group_methodMap.keySet()) {
					List<Method> entityMethods = group_methodMap.get(group);
					String key = createRepetCacheKey(group, entityMethods)
							+ method.getName() + "_";
					for (int i : fieldNames.keySet()) {
						String fieldName = fieldNames.get(i);
						key = key + generateCacheKey(fieldName);
					}
					groupRepetCacheKeys.put(group, key);
					cacheKey_methodMap.put(key, entityMethods);
					System.out.println("unformateGroupRepetCacheKey:" + key);
				}
				unformateGroupRepetCacheKeys.put(method, groupRepetCacheKeys);
				unformateGroupRepetCacheKeyMethod.put(method,
						cacheKey_methodMap);
				logger.info(entityClass
						+ "class:{},method:{},unformateGroupRepetCacheKey:{}",
						new Object[] { entityClass.getName(), method.getName(),
								unformateGroupRepetCacheKeys });
			}
		}

		// 找出size ,cursor对应的参数序列
		Map<Integer, List<LinkedHashMap<Integer, String>>> timelineType_paramsMaps = new HashMap<Integer, List<LinkedHashMap<Integer, String>>>();
		for (Method method : methods) {
			TimelineMethod timelineMethod = method
					.getAnnotation(TimelineMethod.class);
			if (null != timelineMethod) {

				int sizeIdx = getParamsIndex(method,
						TimelineMethodParamEnum.size);
				if (sizeIdx >= 0) {
					sizeIndex.put(method, sizeIdx);
				}
				int cursorIdx = getParamsIndex(method,
						TimelineMethodParamEnum.cursor);
				if (cursorIdx >= 0) {
					cursorIndex.put(method, cursorIdx);
				}
				int sessionIdx = getParamsIndex(method,
						TimelineMethodParamEnum.session);
				if (sessionIdx >= 0) {
					sessionIndex.put(method, sessionIdx);
				}

				LinkedHashMap<Integer, String> paramsMap = getParamsFields(method);
				paramsIndexMap.put(method, paramsMap);

				// 检测 同一组的参数是否相同
				int timelineType = timelineMethod.timelineType();
				List<LinkedHashMap<Integer, String>> paramsMaps = timelineType_paramsMaps
						.get(timelineType);
				if (paramsMaps == null) {
					paramsMaps = new ArrayList<LinkedHashMap<Integer, String>>();
					timelineType_paramsMaps.put(timelineType, paramsMaps);
				} else {
					LinkedHashMap<Integer, String> oldParamsMap = paramsMaps
							.get(0);
					boolean iserror = false;
					if (oldParamsMap.keySet().size() != paramsMap.keySet()
							.size()) {
						iserror = true;
					}
					for (Integer argIndex : oldParamsMap.keySet()) {
						if (!oldParamsMap.get(argIndex).equals(
								paramsMap.get(argIndex))) {
							iserror = true;
						}
					}
					if (iserror) {
						throw new IllegalClassException("timelineType:"
								+ timelineType
								+ ",params error !在同一个timelineType内,参数必须一致");
					}

				}
				paramsMaps.add(paramsMap);
			}
		}

		if (sortMethod == null) {
			throw new IllegalClassException("where is your sortMethod!!!");
		}

	}

	// =======================业务逻辑相关===============================

	/**
	 * 根据实体对象获取id
	 *
	 * @param entity
	 * @return
	 */
	protected abstract Object getIdFromEntity(T entity);

	/**
	 * 根据id获取实体对象
	 *
	 * @param id
	 * @return
	 */
	protected abstract T getEntityFromId(Object id);

	/**
	 * 聚合数据
	 *
	 * @param set
	 *            收集到这个集合里面来
	 * @param size
	 *            需要收集的大小
	 * @param timelineType
	 *            对应第几个timelinetype
	 * @param params
	 *            timeline方法对应的呃参数
	 * @throws Exception
	 */
	public abstract void gatherResult(TreeSet<T> set, int size,
			int timelineType, List<Object> params) throws Exception;

	/**
	 * 过滤内容(在内容重建的时间)。一般静态过滤的时候需要。（如针对所有人的，不怎么变得过滤规则）
	 *
	 * @param unfilterResult
	 *            TreeSet 里面放的是对象
	 * @return
	 */
	public abstract TreeSet<T> filterResultOnRebuild(TreeSet<T> unfilterResult,
			int timelineType, List<Object> params);

	/**
	 * 过滤内容(在已经获取到对象的时候)。一般动态过滤的时候需要。（如针对个人的，比如过滤掉我看过的）
	 *
	 * @param unfilterIds
	 *            linkedHashSet 里面放的是id
	 * @return
	 */
	public abstract Set<Object> filterResultOnGetResult(
			Set<Object> unfilterIds, int timelineType, List<Object> params,
			TimelineSession session);

	/**
	 * 增加一个对象到对应的timeline缓存
	 *
	 * @param entity
	 *            实体对象
	 * @param timelineType
	 *            在timeline方法上注解所对应的timelineType
	 * @param args
	 *            参数 ，需要和timeline方法所传入的参数顺序保持一致
	 * @throws Exception
	 */
	public void addOne(T entity, int timelineType, Object[] args)
			throws Exception {
		Method method = getListMethodBytimelineType(timelineType);
		String cacheKey = getCacheKey(method, args);
		Map<Integer, String> groupRepetCacheKeyMap = getGroupRepetCacheKeyMap(
				method, args);
		Map<String, List<Method>> groupRepetCacheKeyMethod = getGroupRepetCacheKeyMethod(
				method, args);
		addOne(timelineType, getparams(method, args), entity, cacheKey,
				groupRepetCacheKeyMap, groupRepetCacheKeyMethod);
	}

	/**
	 * 从一个timeline缓存快删除掉对象
	 *
	 * @param entity
	 *            实体对象
	 * @param timelineType
	 *            在timeline方法上注解所对应的timelineType
	 * @param args
	 *            参数 ，需要和timeline方法所传入的参数顺序保持一致
	 * @throws Exception
	 */
	public void deleteOne(T entity, int timelineType, Object[] args)
			throws Exception {
		Method method = getListMethodBytimelineType(timelineType);
		String cacheKey = getCacheKey(method, args);
		Map<Integer, String> groupRepetCacheKeyMap = getGroupRepetCacheKeyMap(
				method, args);
		Map<String, List<Method>> groupRepetCacheKeyMethod = getGroupRepetCacheKeyMethod(
				method, args);
		deleteOne(entity, cacheKey, groupRepetCacheKeyMap,
				groupRepetCacheKeyMethod);

	}

	/**
	 * 重新建立缓存
	 *
	 * @param timelineType
	 *            在timeline方法上注解所对应的timelineType
	 * @param args
	 *            参数 ，需要和timeline方法所传入的参数顺序保持一致
	 * @throws Exception
	 */
	public void rebuild(boolean cleanIgnore,int timelineType, Object[] args) throws Exception {
		Method method = getListMethodBytimelineType(timelineType);
		String cacheKey = getCacheKey(method, args);
		Map<Integer, String> groupRepetCacheKeyMap = getGroupRepetCacheKeyMap(
				method, args);
		Map<String, List<Method>> groupRepetCacheKeyMethod = getGroupRepetCacheKeyMethod(
				method, args);
		rebuild(cleanIgnore,timelineType, getparams(method, args), cacheKey,
				groupRepetCacheKeyMap, groupRepetCacheKeyMethod);
	}

	/**
	 * 增加一个内容
	 */
	protected void addOne(int methodInex, List<Object> params, T timeline,
			String cacheKey, Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)
			throws Exception {
		// 检测 所有缓存key必须都存在 如果不存在则重建。
		// 有可能hash后repetCacheKey或者dataCacheKey不在一个节点上，而其中的节点down了，会影响逻辑。
		boolean allKeysIsOk = true;
		boolean cleanIgnore=false;
		
		//如果不存在排重缓存也重建
		for (String key : groupRepetCacheKeyMap.values()) {
			if (!cache.exists(key)) {
				logger.info("rebuild on add groupRepetCacheKeyMap not exists");
				allKeysIsOk = false;
				cleanIgnore=true;
			}
		}
		//如果不存在cacheKey 重建
		if (!cache.exists(cacheKey)) {
			logger.info("rebuild on add cacheKey not exists");
			allKeysIsOk = false;
			cleanIgnore=false;
		}
		// 超出最大值也需要重建
		if (cache.zcard(cacheKey) > DEFAULT_MAX_LIST_SIZE) {
			logger.info("rebuild on add over max list size");
			allKeysIsOk = false;
			cleanIgnore=true;
		}

		if (!allKeysIsOk) {
			rebuild(cleanIgnore,methodInex, params, cacheKey, groupRepetCacheKeyMap,
					groupRepetCacheKeyMethod);
			return;
		}
		
		//如果有人重建，则等待重建完成。
		waitForExclusiveCacheKey(getExclusiveCacheKey(cacheKey), "on add message",1000);

		Object id = getIdFromEntity(timeline);
		boolean hasThisObj = isThisEntityInCache(timeline,
				groupRepetCacheKeyMap, groupRepetCacheKeyMethod);
		long sort = ReflectUtil.getLongValueForLongOrInt(sortMethod
				.invoke(timeline));
		if (hasThisObj) {
			// 取出oldId 然后 再算出所有的cacheKeys 再依次删除
			// 视频1：vid=1 title=111
			// 视频2：vid=2 title=222
			// 现在插入：vid=1 title=222.需要把之前的两个都给删除掉。
			List<Object> oldIds = new ArrayList<Object>();
			for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
				List<Method> methods = groupRepetCacheKeyMethod
						.get(groupRepetCacheKey);
				String uniqueValue = getUniqueValueFormMethods(timeline,
						methods);
				Object oldId = cache
						.hGetObject(groupRepetCacheKey, uniqueValue,null);
				if(oldId==null){
					continue;
				}
				oldIds.add(oldId);
				T entity = getEntityFromId(oldId);
				removeFromGroupRepetCache(entity, groupRepetCacheKeyMap,
						groupRepetCacheKeyMethod);
			}
			for (Object oldId : oldIds) {
				cache.zremObject(cacheKey, oldId);
			}
		}

		addToGroupRepetCache(timeline, groupRepetCacheKeyMap,
				groupRepetCacheKeyMethod);
		cache.zaddObject(cacheKey, sort, id);

	}

	/**
	 * 删除一个内容
	 */
	protected void deleteOne(T timeline, String cacheKey,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)
			throws Exception {
		removeFromGroupRepetCache(timeline, groupRepetCacheKeyMap,
				groupRepetCacheKeyMethod);
		cache.zremObject(cacheKey, getIdFromEntity(timeline));
	}
	
	
	

    //获取唯一排他锁
	protected String getExclusiveCacheKey(String cacheKey){
		return cacheKey+"_exclusiveCacheKey";
	}
	
	private void waitForExclusiveCacheKey(String key,String logMsg,int times){
		if(!cache.exists(key)){
			return;
		}
		long start=System.currentTimeMillis();
		for(int i=0;i<times;i++){
			if(!cache.exists(key)){
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("waitForExclusiveCacheKey key:{},logMsg:{},time:{}",new Object[]{key,logMsg,(System.currentTimeMillis()-start)});
		
	}
	
	
	/**
	 * 重建缓存
	 * 1.锁住，只能单个进行
	 * 2.判断是否有缓存，并且是否强制清楚（强制清除主要是人为操作，聚合关系产生变化）
	 * 3.如果有缓存，但是强制清除（聚合关系变化），那么需要重建缓存
	 * 4.如果没有缓存，（缓存失效），需要重建缓存
	 * 5.如果有缓存，但是没有强制清除（缓存已经构建），那么不需要，直接返回。
	 * 重建缓存逻辑：
	 * 1.聚合所有未过滤的数据
	 * 2.进行过滤
	 * 3.判断是否强制清除，如果强制清除那么删除掉缓存。
	 * 4.如果有缓存，不是强制清除--（不符合逻辑，应该不会出现。记录一个错误日志，然后返回。）
	 * 5.删除重复键缓存
	 * 6.去重
	 * 7.填入数据。
	 * 
	 */
	protected void rebuild(boolean cleanIgnore,int timelineType, List<Object> params,
			String cacheKey, Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod) {
		long startTime=System.currentTimeMillis();
		logger.info("rebuild begin cleanIgnore:{} cacheKey:{}",cleanIgnore,cacheKey);
		ZkLock lock = null;
		try {
			lock = ZkLock.getAndLock(getZookeeperAddress(), "hyperSpace", cacheKey, true, 30000, 30000);
			if(cache.exists(cacheKey)&&!cleanIgnore){
				logger.info("rebuild end exists cacheKey:{},time:{}",cacheKey,(System.currentTimeMillis()-startTime));
				return;
			}
			
			if (!cache.exists(cacheKey)||cleanIgnore) {
				logger.info("rebuild gather 1 result begin cacheKey:{},time:{}",cacheKey,(System.currentTimeMillis()-startTime));
				@SuppressWarnings({ "rawtypes", "unchecked" })
				TreeSet<T> unFilterResult = new TreeSet<T>(new Comparator() {
					@Override
					public int compare(Object arg0, Object arg1) {
						long value = 0;
						try {
							value = ReflectUtil
									.getLongValueForLongOrInt(sortMethod
											.invoke(arg1))
									- ReflectUtil
											.getLongValueForLongOrInt(sortMethod
													.invoke(arg0));
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (value >= 0) {
							value = 1;
						}else{
							value =-1;
						}
						return (int) value;
					}
				});
				// 收集数据
				gatherResult(unFilterResult, DEFAULT_INIT_LIST_SIZE,
						timelineType, params);
				// 业务过滤
				TreeSet<T> repetResult = filterResultOnRebuild(unFilterResult,
						timelineType, params);

				if(repetResult!=null)
				logger.info("rebuild gather 2 result end cacheKey:{},repetResults:{},time:{}",new Object[]{cacheKey,repetResult.size(),(System.currentTimeMillis()-startTime)});

				
				
				Map<Integer, String> tempGroupRepetCacheKeyMap=new HashMap<Integer, String>(); 
				Map<String, List<Method>>  tempGroupRepetCacheKeyMethod=new HashMap<String, List<Method>>(); 
				
				for ( Integer groupId: groupRepetCacheKeyMap.keySet()) {
					
					String groupRepetCacheKey=groupRepetCacheKeyMap.get(groupId);
					List<Method> methods = groupRepetCacheKeyMethod
							.get(groupRepetCacheKey);
					String tempGroupRepetCacheKey=groupRepetCacheKey+"_"+UUID.randomUUID().toString();
					tempGroupRepetCacheKeyMap.put(groupId, tempGroupRepetCacheKey);
					tempGroupRepetCacheKeyMethod.put(tempGroupRepetCacheKey, methods);
				}
				
				
				List<T> unRepetResult=getUnRepetResult(repetResult, tempGroupRepetCacheKeyMap, tempGroupRepetCacheKeyMethod);
				
				logger.info("rebuild remove RepetResult  end 3  cacheKey:{},time:{}",new Object[]{cacheKey,(System.currentTimeMillis()-startTime)});
				
					
				if(!cache.exists(cacheKey)||cleanIgnore){
					boolean lockSuccess=cache.setStringIfNotExist(getExclusiveCacheKey(cacheKey), 600, "true")==1?true:false;
					if(lockSuccess){
						logger.info("rebuild gather 4 deleteCacheKey cacheKey:{},time:{}",new Object[]{cacheKey,(System.currentTimeMillis()-startTime)});
						cache.delete(cacheKey);
						deleteGroupRepetCacheKeys(groupRepetCacheKeyMap);
					}else{
						logger.info("rebuild gather 4 lockFail cacheKey:{},time:{}",new Object[]{cacheKey,(System.currentTimeMillis()-startTime)});
						return ;
					}
				}else {
					logger.error("rebuild  error Error ERROR rebulidError cacheExites");
					return ;
				}
				
				Map<String,Map<String,Object>> map=new HashMap<String, Map<String,Object>>();
//				Map<Double, Object> members = new HashMap<Double, Object>();
				Map<Object, Double> members = new HashMap<Object, Double>();
				// 存储
				for (int i = 0; (i < unRepetResult.size() && i < DEFAULT_INIT_LIST_SIZE); i++) {
					T entity = unRepetResult.get(i);
					long sort = ReflectUtil.getLongValueForLongOrInt(sortMethod
							.invoke(entity));
					Object id = getIdFromEntity(entity);
//					if (members.keySet().contains(new Double(sort))) {
//						this.cache.zaddObject(cacheKey, new Double(sort), id);
//					} else {
//						members.put(new Double(sort), id);
//					}
					members.put(id, new Double(sort));
					addToGroupRepetCacheAndReturn(map,entity, groupRepetCacheKeyMap,
							groupRepetCacheKeyMethod);
				}
				
				for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
					this.cache.hMultiSetObject(groupRepetCacheKey, map.get(groupRepetCacheKey));
				}
				
				this.cache.zaddMultiObject(cacheKey, members);
				this.cache.expire(cacheKey, DEFAULT_EXPIRED_TIME);
				this.cache.delete(getExclusiveCacheKey(cacheKey));
				logger.info("rebuild ok  cacheKey:{},time:{}",new Object[]{cacheKey,(System.currentTimeMillis()-startTime)});
			
			}
		} catch (Exception e) {
			try {
				cache.delete(cacheKey);
				deleteGroupRepetCacheKeys(groupRepetCacheKeyMap);
			} catch (Exception e1) {
				e1.printStackTrace();
				logger.error("rebulie error",e);
			}
			throw new PermissionDeniedDataAccessException("", e);
		} finally {
			if(lock!=null)
			lock.release();
		}
	}

	public Set<Object> timeline_getPageList(Method method, List<Object> params,
			String cacheKey, Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod, double cursor,
			int size, TimelineSession session) {

		int timelineType = method.getAnnotation(TimelineMethod.class)
				.timelineType();
		double rate = DEFAULT_RATE;
		double minScore = 0, maxScore = 0;
		Set<Object> set = null;
		maxScore = (cursor == 0) ? Double.MAX_VALUE : cursor;
		waitForExclusiveCacheKey(getExclusiveCacheKey(cacheKey), "get TimeLineMessage",50);
		set = this.cache.zrevrangeByScoreObject(cacheKey, maxScore, minScore,
				0, (int) (size * rate),null);
		// 初始化list
		if ((null == set || set.size() == 0) && !cache.exists(cacheKey)) {
			rebuild(false,timelineType, params, cacheKey, groupRepetCacheKeyMap,
					groupRepetCacheKeyMethod);
			set = this.cache.zrevrangeByScoreObject(cacheKey, maxScore,
					minScore, 0, (int) (size * rate),null);
		}

		// 支持动态过滤
		boolean noMoreData = false;
		for (int i = 1; i <= Integer.MAX_VALUE; i++) {
			if (set.size() < (int) (size * rate)) {
				noMoreData = true;
			}
			set = filterResultOnGetResult(set, timelineType, params, session);
			if (i == Integer.MAX_VALUE || noMoreData || set.size() > size) {
				break;
			} else {
				if (i >= 3) { // 超过3次补全，则取10*N倍大小的数据
					rate = rate + 10 * (i - 1);
					set = this.cache.zrevrangeByScoreObject(cacheKey, maxScore,
							minScore, 0, (int) (size * rate),null);
				} else {
					rate = rate + 2 * (i - 1);
					set = this.cache.zrevrangeByScoreObject(cacheKey, maxScore,
							minScore, 0, (int) (size * rate),null);
				}
			}
		}

		return set;
	}

	public int timeline_getCount(int timelineType, List<Object> params,
			String cacheKey, Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod, double cursor) {
		if (!cache.exists(cacheKey)) {
			return 0;
		}
		return cache.zcount(cacheKey, cursor, Double.MAX_VALUE).intValue() - 1;
	}

	public Void timeline_rebuild(int timelineType, List<Object> params,
			String cacheKey, Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod) {
		rebuild(true,timelineType, params, cacheKey, groupRepetCacheKeyMap,
				groupRepetCacheKeyMethod);
		return null;
	}

	// =======================工具相关===============================

	public List<Object> getparams(Method method, Object[] args) {
		List<Object> result = new ArrayList<Object>();
		Map<Integer, String> params = getParamsIndexMap().get(method);
		for (int i : params.keySet()) {
			result.add(args[i]);
		}
		return result;
	}

	/**
	 * 根据“去重逻辑”同一组的多个属性，构建出唯一值，用来去重用
	 *
	 * @param entity
	 * @param methods
	 * @return
	 * @throws Exception
	 */
	private String getUniqueValueFormMethods(T entity, List<Method> methods)
			throws Exception {
		List<Object> result = new ArrayList<Object>();
		for (Method method : methods) {
			result.add(method.invoke(entity));
		}
		return JsonUtils.format(result);
	}

	/**
	 * 根据timelineType来获取对应的timeline方法
	 *
	 * @param timelineIndex
	 * @return
	 */
	public Method getListMethodBytimelineType(int timelineType) {
		Map<Method, String> map = getUnformateCacheKeys();
		for (Method method : map.keySet()) {
			if (timelineType == method.getAnnotation(TimelineMethod.class)
					.timelineType()) {
				return method;
			}
		}
		return null;
	}

	/**
	 * 根据timeline方法和传入的值 找出对应的缓存key
	 *
	 * @param method
	 *            timeline方法
	 * @param args
	 *            传入的值
	 * @return
	 */
	public String getCacheKey(Method method, Object[] args) {
		Map<Method, String> map = getUnformateCacheKeys();
		String cacheKey = map.get(method);
		cacheKey = formatCacheKey(cacheKey, method, args);
		return cacheKey;
	}

	/**
	 * 格式化缓存key
	 *
	 * @param cacheKey
	 * @param method
	 * @param args
	 * @return
	 */
	public String formatCacheKey(String cacheKey, Method method, Object[] args) {
		Map<Integer, String> params = getParamsIndexMap().get(method);
		if (params != null) {
			for (int i : params.keySet()) {
				cacheKey = cacheKey
						.replaceAll(
								generateCacheKey(params.get(i)),
								replaceCacheKeyValue(params.get(i),
										args[i].toString()));
			}
		}
		return cacheKey;
	}

	/**
	 * 根据timeline方法和传入的值 找出对应的GroupRepetCacheKeyMap（组ID-格式化好的缓存去重key）
	 *
	 * @param method
	 *            timeline方法
	 * @param args
	 *            传入的值
	 * @return
	 */
	public Map<Integer, String> getGroupRepetCacheKeyMap(Method method,
			Object[] args) {
		Map<Integer, String> groupRepetCacheKeyMap = new HashMap<Integer, String>();
		Map<Integer, String> unformatGroupRepetCacheKeyMap = getUnformateGroupRepetCacheKeys()
				.get(method);
		for (Integer group : unformatGroupRepetCacheKeyMap.keySet()) {
			String unformatGroupRepetCacheKey = unformatGroupRepetCacheKeyMap
					.get(group);
			String formatGroupRepetCacheKey = formatCacheKey(
					unformatGroupRepetCacheKey, method, args);
			groupRepetCacheKeyMap.put(group, formatGroupRepetCacheKey);
		}
		return groupRepetCacheKeyMap;
	}

	/**
	 * 根据timeline方法和传入的值
	 * 找出对应的GroupRepetCacheKeyMethod（格式化好的缓存去重key-对应的实体类的属性读方法）
	 *
	 * @param method
	 *            timeline方法
	 * @param args
	 *            传入的值
	 * @return
	 */
	public Map<String, List<Method>> getGroupRepetCacheKeyMethod(Method method,
			Object[] args) {
		Map<String, List<Method>> groupRepetCacheKeyMethod = new HashMap<String, List<Method>>();
		Map<Integer, String> unformatGroupRepetCacheKeyMap = getUnformateGroupRepetCacheKeys()
				.get(method);
		Map<String, List<Method>> unformatGroupRepetCacheKeyMethod = getUnformateGroupRepetCacheKeyMethod()
				.get(method);
		for (Integer group : unformatGroupRepetCacheKeyMap.keySet()) {
			String unformatGroupRepetCacheKey = unformatGroupRepetCacheKeyMap
					.get(group);
			String formatGroupRepetCacheKey = formatCacheKey(
					unformatGroupRepetCacheKey, method, args);
			groupRepetCacheKeyMethod.put(formatGroupRepetCacheKey,
					unformatGroupRepetCacheKeyMethod
							.get(unformatGroupRepetCacheKey));
		}
		return groupRepetCacheKeyMethod;
	}

	/**
	 * 删除所有的
	 *
	 * @param groupRepetCacheKeyMap
	 */
	private void deleteGroupRepetCacheKeys(
			Map<Integer, String> groupRepetCacheKeyMap) {
		for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
			cache.delete(groupRepetCacheKey);
		}
	}

	/**
	 * 判断是否已经存在在这个去重缓存里面
	 *
	 * @param entity
	 * @param groupRepetCacheKeyMap
	 * @param groupRepetCacheKeyMethod
	 * @return
	 * @throws Exception
	 */
	private boolean isThisEntityInCache(T entity,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)
			throws Exception {
		boolean hasTHisOBj = false;
		for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
			List<Method> methods = groupRepetCacheKeyMethod
					.get(groupRepetCacheKey);
			String value = getUniqueValueFormMethods(entity, methods);
			if (cache.hExists(groupRepetCacheKey, value)) {
				hasTHisOBj = true;
			} else {
				Object id = getIdFromEntity(entity);
				cache.hSetObject(groupRepetCacheKey, value, id);
			}
		}
		return hasTHisOBj;
	}
	
	
	private List<T> getUnRepetResult(TreeSet<T> repetResult,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)throws Exception{
		List<T> unRepetResult=new ArrayList<T>();
		
		Map<String,HashSet<String>> map=new HashMap<String, HashSet<String>>();
		
		for(T entity: repetResult){
			boolean hasTHisOBj = false;
			for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
				List<Method> methods = groupRepetCacheKeyMethod
						.get(groupRepetCacheKey);
				String value = getUniqueValueFormMethods(entity, methods);
				
				HashSet<String> set=	map.get(groupRepetCacheKey);
				if(set==null){
					set=new HashSet<String>();
					map.put(groupRepetCacheKey, set);
				}
				if (set.contains( value)) {
					hasTHisOBj = true;
					break;
				} else {
					set.add(value);
				}
			}
			if(!hasTHisOBj){
				unRepetResult.add(entity);
			}else{
				logger.debug("gather result remove repetResult:{}",entity);
			}
		}
		return unRepetResult;
		
	}
	

	/**
	 * 移除掉对象对应的去重缓存
	 *
	 * @param entity
	 * @param groupRepetCacheKeyMap
	 * @param groupRepetCacheKeyMethod
	 * @throws Exception
	 */
	private void removeFromGroupRepetCache(T entity,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)
			throws Exception {
		for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
			List<Method> methods = groupRepetCacheKeyMethod
					.get(groupRepetCacheKey);
			String value = getUniqueValueFormMethods(entity, methods);
			cache.hDelete(groupRepetCacheKey, value);
		}
	}

	/**
	 * 添加对象到去重缓存
	 *
	 * @param entity
	 * @param groupRepetCacheKeyMap
	 * @param groupRepetCacheKeyMethod
	 * @throws Exception
	 */
	private void addToGroupRepetCache(T entity,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)
			throws Exception {
		for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
			List<Method> methods = groupRepetCacheKeyMethod
					.get(groupRepetCacheKey);
			String value = getUniqueValueFormMethods(entity, methods);
			Object id = getIdFromEntity(entity);
			cache.hSetObject(groupRepetCacheKey, value, id);
		}
	}
	
	private Map<String,Map<String,Object>> addToGroupRepetCacheAndReturn(
			Map<String,Map<String,Object>> map,T entity,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod)
			throws Exception {
		
		for (String groupRepetCacheKey : groupRepetCacheKeyMap.values()) {
			List<Method> methods = groupRepetCacheKeyMethod
					.get(groupRepetCacheKey);
			String value = getUniqueValueFormMethods(entity, methods);
			Object id = getIdFromEntity(entity);
			
			Map<String,Object> subMap=map.get(groupRepetCacheKey);
			if(subMap==null){
				subMap=new HashMap<String, Object>();
				map.put(groupRepetCacheKey, subMap);
			}
			subMap.put(value, id);
		}
		return map;
		
	}

	// =====================从abstractCacheBaseDao拷贝过来的方法，基本没改=====================

	/**
	 * 根据方法获取对应参数的名字（或者是注解锁表示的字符串）。此名字必须要和实体类的保持一致，如果不一致，请用注解标识。
	 *
	 * @param method
	 * @return
	 */
	private LinkedHashMap<Integer, String> getParamsFields(Method method) {
		LinkedHashMap<Integer, String> list = new LinkedHashMap<Integer, String>();
		String[] result = ReflectUtil.getMethodParamNames(method);
		for (int i = 0; i < result.length; i++) {
			String paramName = result[i];
			if (paramName.equals("size") || paramName.equals("cursor")
					|| paramName.equals("session")) {
				result[i] = "";
			}
		}
		// 加上注解的会置换掉参数
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++) {
			// 没有的话 也会有占位
			if (annotations[i].length > 0) {
				Annotation one = annotations[i][0];
				if (one instanceof TimelineMethodParam) {
					TimelineMethodParam param = (TimelineMethodParam) one;
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

	/**
	 *
	 * @return
	 */
	public String generateCacheKey(String name) {
		return "_" + name + "_@" + name;
	}

	public String replaceCacheKeyValue(String name, String value) {
		return "_" + name + "_" + value;
	}

	private int getParamsIndex(Method method, TimelineMethodParamEnum paramEnum) {
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
				if (one instanceof TimelineMethodParam) {
					TimelineMethodParam param = (TimelineMethodParam) one;
					if (param.paramEnum() == paramEnum) {
						index = i;
					}
				}
			}
		}
		return index;
	}

}
