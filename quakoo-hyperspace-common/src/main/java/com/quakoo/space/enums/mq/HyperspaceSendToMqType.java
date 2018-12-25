package com.quakoo.space.enums.mq;

/**
 * 暂时不支持
 * @author LiYongbiao1
 *
 */
public enum HyperspaceSendToMqType {

    afterInsert(1, "insert"), afterUpdate(2, "update"), afterdelete(3, "delete");

    HyperspaceSendToMqType(int id, String type) {
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
