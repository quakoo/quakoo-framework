package com.quakoo.space.enums;

public enum IdentityType {
    identity(1, "identity"), // seq自增
    origin_indentity(2, "origin_indentity"),// 数据库自增
    human(3, "human");// 用户自己弄

    IdentityType(int id, String type) {
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
