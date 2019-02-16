package com.quakoo.baseFramework.ali.media;


public class NioConnectInfo {

    private long uid;

    private long rid;

    private long connectTime;

    public NioConnectInfo() {
    }

    public NioConnectInfo(long uid, long rid, long connectTime) {
        this.uid = uid;
        this.rid = rid;
        this.connectTime = connectTime;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }

    public long getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(long connectTime) {
        this.connectTime = connectTime;
    }
}
