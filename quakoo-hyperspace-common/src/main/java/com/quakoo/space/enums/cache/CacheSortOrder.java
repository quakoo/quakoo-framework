package com.quakoo.space.enums.cache;

/**
 * 排序类型，正序和倒序
 * @author LiYongbiao1
 *
 */
public enum CacheSortOrder {
	asc(0, "asc"), //
	desc(1, "desc");

	CacheSortOrder(int id, String name) {
		this.id = id;
		this.name = name;
	}

	private final int id;

	private final String name;

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
