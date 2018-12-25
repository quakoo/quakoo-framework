package com.quakoo.baseFramework.model.pagination;

import java.util.HashMap;
import java.util.Map;

public class PagerSession {

	private Map<String, Map<Long, Object>> id_map = new HashMap<String, Map<Long, Object>>();// 用于存储key-value类型的数据，其中key的类型是long类型（大部分是Id）

	public Map<String, Map<Long, Object>> getId_map() {
		return id_map;
	}
}
