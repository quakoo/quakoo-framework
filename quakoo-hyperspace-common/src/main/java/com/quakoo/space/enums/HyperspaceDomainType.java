package com.quakoo.space.enums;

public enum HyperspaceDomainType {

    mainDataStructure(1, "main"), // 数据结构-主表
    listDataStructure(2, "list"); // 数据结构-list表

    HyperspaceDomainType(int id, String type) {
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
