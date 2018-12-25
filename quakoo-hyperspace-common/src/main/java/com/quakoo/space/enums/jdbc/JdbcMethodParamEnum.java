package com.quakoo.space.enums.jdbc;

/***
 * dao方法的参数
 * @author LiYongbiao1
 *
 */
public enum JdbcMethodParamEnum {
	NULL(-1, "null"), //
	shardingId(0, "shardingId");

	JdbcMethodParamEnum(int id, String name) {
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
