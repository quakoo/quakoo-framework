package com.quakoo.space.enums.index;

public enum IndexMethodParamEnum {

    size(1, "size"), //

    cursor(2, "cursor"),

    search(3, "search");

    IndexMethodParamEnum(int id, String name) {
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
