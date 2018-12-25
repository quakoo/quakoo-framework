package com.quakoo.space.enums;

public enum HyperspaceType {

	jdbc(1, "jdbc"), cache(2, "cache"), index(3, "index");

	HyperspaceType(int id, String type) {
		this.id = id;
		this.type = type;
	}

	private final int id;

	private final String type;

	public int getId() {
		return id;
	}

	public String getType() {
		return type;
	}
}
