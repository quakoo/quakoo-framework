package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class UserStreamQueue implements Serializable {

    private long uid;

    private int type;

    private long thirdId;

    private long mid;

    private long authorId;

    private double sort;

    public UserStreamQueue() {}

    public UserStreamQueue(UserStream stream) {
        this.uid = stream.getUid();
        this.type = stream.getType();
        this.thirdId = stream.getThirdId();
        this.mid = stream.getMid();
        this.authorId = stream.getAuthorId();
        this.sort = stream.getSort();
    }


    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getThirdId() {
        return thirdId;
    }

    public void setThirdId(long thirdId) {
        this.thirdId = thirdId;
    }

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
    }

    public long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    public double getSort() {
        return sort;
    }

    public void setSort(double sort) {
        this.sort = sort;
    }
}
