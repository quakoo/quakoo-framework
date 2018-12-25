package com.quakoo.baseFramework.excel;

public enum ExcelReadType {

    common(1, "common"),
    picture(2, "picture");

    ExcelReadType(int id, String type) {
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
