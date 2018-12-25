package com.quakoo.space.model.timeline;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TimelineSession implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8992203612944259001L;

	private Map<String, Map<Long, Object>> id_map = new HashMap<String, Map<Long, Object>>();// 用于存储key-value类型的数据，其中key的类型是long类型（大部分是Id）

	public Map<String, Map<Long, Object>> getId_map() {
		return id_map;
	}

}
