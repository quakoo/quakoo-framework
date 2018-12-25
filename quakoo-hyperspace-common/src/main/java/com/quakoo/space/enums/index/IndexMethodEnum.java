package com.quakoo.space.enums.index;

public enum IndexMethodEnum {

    getIndexes(0, "getIndexes"),

    insertIndex(1, "insertIndex"),

    loadIndex(2, "loadIndex"),

    deleteIndex(3, "deleteIndex"),

    searchIndexes(4, "searchIndexes");


    IndexMethodEnum(int id, String name) {
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
