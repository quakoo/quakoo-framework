package com.quakoo.space.aop.timeline;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.space.annotation.timeline.TimelineMethod;
import com.quakoo.space.enums.timeline.TimelineMethodEnum;
import com.quakoo.space.model.timeline.TimelineSession;
import com.quakoo.space.timeline.AbstractTimelineService;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimelineCommonMethodInterceptor implements MethodInterceptor {

	String projectName;

	Logger logger = LoggerFactory
			.getLogger(TimelineCommonMethodInterceptor.class);

	private double getCursor(Object object, Method method, Object[] args) {
		AbstractTimelineService<?> service = (AbstractTimelineService<?>) object;
		int cursor_index = service.getCursorIndex().get(method);
		double cursor = (Double) args[cursor_index];
		return cursor;
	}

	private int getSize(Object object, Method method, Object[] args) {
		AbstractTimelineService<?> service = (AbstractTimelineService<?>) object;
		int size_index = service.getSizeIndex().get(method);
		int size = (Integer) args[size_index];
		return size;
	}

	private TimelineSession getSession(Object object, Method method,
                                       Object[] args) {
		AbstractTimelineService<?> service = (AbstractTimelineService<?>) object;
		Integer session_index = service.getSessionIndex().get(method);
		if (null == session_index) {
			return null;
		}
		return (TimelineSession) args[session_index];
	}

	// private Object handleGetCount(Object object, Method method, Object[]
	// args) throws Throwable {}

	private Object handleGetPageList(Object object, Method method, Object[] args)
			throws Throwable {
		AbstractTimelineService<?> service = (AbstractTimelineService<?>) object;
		String cacheKey = service.getCacheKey(method, args);
		Map<Integer, String> groupRepetCacheKeyMap = service
				.getGroupRepetCacheKeyMap(method, args);
		Map<String, List<Method>> groupRepetCacheKeyMethod = service
				.getGroupRepetCacheKeyMethod(method, args);
		int size = getSize(object, method, args);
		double cursor = getCursor(object, method, args);
		TimelineSession session = getSession(object, method, args);
		List<Object> params = service.getparams(method, args);

		logger.info(method.getDeclaringClass().getSimpleName() + ","
				+ method.getName() + "==============cacheKey:" + cacheKey
				+ ",cursor:" + cursor + ",size:" + size + ",params:" + params);
		Set<Object> set = service.timeline_getPageList(method, params,
				cacheKey, groupRepetCacheKeyMap, groupRepetCacheKeyMethod,
				cursor, size, session);
		return set;
	}

	private Object handleGetCount(int timelineType, Object object,
			Method method, Object[] args) {
		AbstractTimelineService<?> service = (AbstractTimelineService<?>) object;
		Method listMethod = service.getListMethodBytimelineType(timelineType);
		String cacheKey = service.getCacheKey(listMethod, args);
		Map<Integer, String> groupRepetCacheKeyMap = service
				.getGroupRepetCacheKeyMap(listMethod, args);
		Map<String, List<Method>> groupRepetCacheKeyMethod = service
				.getGroupRepetCacheKeyMethod(listMethod, args);
		double cursor = getCursor(object, method, args);
		List<Object> params = service.getparams(listMethod, args);
		return service.timeline_getCount(timelineType, params, cacheKey,
				groupRepetCacheKeyMap, groupRepetCacheKeyMethod, cursor);
	}

	private Object handleRebuild(int timelineType, Object object,
			Method method, Object[] args) {
		AbstractTimelineService<?> service = (AbstractTimelineService<?>) object;
		Method listMethod = service.getListMethodBytimelineType(timelineType);
		String cacheKey = service.getCacheKey(listMethod, args);
		Map<Integer, String> groupRepetCacheKeyMap = service
				.getGroupRepetCacheKeyMap(listMethod, args);
		Map<String, List<Method>> groupRepetCacheKeyMethod = service
				.getGroupRepetCacheKeyMethod(listMethod, args);
		List<Object> params = service.getparams(listMethod, args);
		return service.timeline_rebuild(timelineType, params, cacheKey,
				groupRepetCacheKeyMap, groupRepetCacheKeyMethod);
	}


	@Override
	public Object intercept(Object object, Method method, Object[] args,
			MethodProxy methodProxy) throws Throwable {
		TimelineMethod timelineMethod = method
				.getAnnotation(TimelineMethod.class);
		if (null != timelineMethod) {
			TimelineMethodEnum methodEnum = timelineMethod.methodEnum();
			if (methodEnum != null && args != null) {
//				logger.info(method.getDeclaringClass().getSimpleName() + ","
//						+ method.getName() + "==============args:"
//						+ Arrays.asList(args));
			}
			if (methodEnum == TimelineMethodEnum.getPageList) {
				return this.handleGetPageList(object, method, args);
			} else if (methodEnum == TimelineMethodEnum.getCount) {
				return this.handleGetCount(timelineMethod.timelineType(),
						object, method, args);
			} else if (methodEnum == TimelineMethodEnum.rebuild) {
				return this.handleRebuild(timelineMethod.timelineType(),
						object, method, args);
			} else {
				return methodProxy.invokeSuper(object, args);
			}
		}
		return methodProxy.invokeSuper(object, args);
	}

}
