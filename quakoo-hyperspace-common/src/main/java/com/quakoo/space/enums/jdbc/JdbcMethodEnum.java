package com.quakoo.space.enums.jdbc;

/**
 * dao方法类型（操作数据库）
 * @author LiYongbiao1
 *
 */
public enum JdbcMethodEnum {

	getList(0, "getList"), //
	getCount(1, "getCount");

	JdbcMethodEnum(int id, String name) {
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
