package com.quakoo.space.timeline;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineCacheInfo implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -9139846118327894017L;

	private String listDataCacheKey;
	private Map<Integer, String> groupRepetCacheKeyMap;
	// list 里面是method的名字
	Map<String, List<String>> groupRepetCacheKeyMethodName;

	public TimelineCacheInfo() {

	}

	public TimelineCacheInfo(String listDataCacheKey,
			Map<Integer, String> groupRepetCacheKeyMap,
			Map<String, List<Method>> groupRepetCacheKeyMethod) {
		super();
		this.listDataCacheKey = listDataCacheKey;
		this.groupRepetCacheKeyMap = groupRepetCacheKeyMap;
		this.groupRepetCacheKeyMethodName = getGroupRepetCacheKeyMethodName(groupRepetCacheKeyMethod);
	}

	public String getListDataCacheKey() {
		return listDataCacheKey;
	}

	public void setListDataCacheKey(String listDataCacheKey) {
		this.listDataCacheKey = listDataCacheKey;
	}

	public Map<Integer, String> getGroupRepetCacheKeyMap() {
		return groupRepetCacheKeyMap;
	}

	public void setGroupRepetCacheKeyMap(
			Map<Integer, String> groupRepetCacheKeyMap) {
		this.groupRepetCacheKeyMap = groupRepetCacheKeyMap;
	}

	public Map<String, List<String>> getGroupRepetCacheKeyMethodName() {
		return groupRepetCacheKeyMethodName;
	}

	public void setGroupRepetCacheKeyMethodName(
			Map<String, List<String>> groupRepetCacheKeyMethodName) {
		this.groupRepetCacheKeyMethodName = groupRepetCacheKeyMethodName;
	}











	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((groupRepetCacheKeyMap == null) ? 0 : groupRepetCacheKeyMap
						.hashCode());
		result = prime
				* result
				+ ((groupRepetCacheKeyMethodName == null) ? 0
						: groupRepetCacheKeyMethodName.hashCode());
		result = prime
				* result
				+ ((listDataCacheKey == null) ? 0 : listDataCacheKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TimelineCacheInfo other = (TimelineCacheInfo) obj;
		if (groupRepetCacheKeyMap == null) {
			if (other.groupRepetCacheKeyMap != null) {
				return false;
			}
		} else if (!groupRepetCacheKeyMap.equals(other.groupRepetCacheKeyMap)) {
			return false;
		}
		if (groupRepetCacheKeyMethodName == null) {
			if (other.groupRepetCacheKeyMethodName != null) {
				return false;
			}
		} else if (!groupRepetCacheKeyMethodName
				.equals(other.groupRepetCacheKeyMethodName)) {
			return false;
		}
		if (listDataCacheKey == null) {
			if (other.listDataCacheKey != null) {
				return false;
			}
		} else if (!listDataCacheKey.equals(other.listDataCacheKey)) {
			return false;
		}
		return true;
	}

	public static Map<String, List<Method>> getGroupRepetCacheKeyMethod(
			Map<String, List<String>> groupRepetCacheKeyMethodName,
			Class entityClass) {
		Map<String, List<Method>> groupRepetCacheKeyMethod = new HashMap<String, List<Method>>();
		for (String key : groupRepetCacheKeyMethodName.keySet()) {
			List<String> methodNames = groupRepetCacheKeyMethodName.get(key);
			List<Method> methods = new ArrayList<Method>();
			for (String methodName : methodNames) {
				try {
					methods.add(entityClass.getMethod(methodName));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			groupRepetCacheKeyMethod.put(key, methods);
		}
		return groupRepetCacheKeyMethod;

	}

	public static Map<String, List<String>> getGroupRepetCacheKeyMethodName(
			Map<String, List<Method>> groupRepetCacheKeyMethod) {
		Map<String, List<String>> groupRepetCacheKeyMethodName = new HashMap<String, List<String>>();
		for (String key : groupRepetCacheKeyMethod.keySet()) {
			List<Method> methods = groupRepetCacheKeyMethod.get(key);
			List<String> methodNames = new ArrayList<String>();
			for (Method method : methods) {
				methodNames.add(method.getName());
			}
			groupRepetCacheKeyMethodName.put(key, methodNames);
		}
		return groupRepetCacheKeyMethodName;
	}
}
