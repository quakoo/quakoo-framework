package com.quakoo.framework.ext.chat.model.param.nio;

public class RecallResponse extends SessionResponse {

    public RecallResponse() {
        super();
        this.type = type_recall;
    }

    private long mid;

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
    }

}
