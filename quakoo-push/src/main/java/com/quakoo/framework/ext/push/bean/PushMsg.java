package com.quakoo.framework.ext.push.bean;

import java.io.Serializable;
import java.util.Map;

public class PushMsg implements Serializable {

    private long uid;
    private String uids;
    private String title;
    private String content;
    private Map<String, String> extra;
    private int platform;
    private int type;

    private long payloadId;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public long getPayloadId() {
        return payloadId;
    }

    public void setPayloadId(long payloadId) {
        this.payloadId = payloadId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUids() {
        return uids;
    }

    public void setUids(String uids) {
        this.uids = uids;
    }
}
