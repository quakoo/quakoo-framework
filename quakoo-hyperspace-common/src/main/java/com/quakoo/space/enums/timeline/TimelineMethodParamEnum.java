package com.quakoo.space.enums.timeline;

public enum TimelineMethodParamEnum {
	NULL(-1, "null"), //
	size(1, "size"), //
	cursor(2, "cursor"), //
	session(3, "session");

	TimelineMethodParamEnum(int id, String name) {
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
