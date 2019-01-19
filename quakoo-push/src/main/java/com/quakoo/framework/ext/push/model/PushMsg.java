package com.quakoo.framework.ext.push.model;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;

public class PushMsg implements Serializable {

    public static final int type_single = 1;
    public static final int type_batch = 2;
    public static final int type_all = 3;

    public static final int status_wait = 0;
    public static final int status_send = 1;


    private long id; // #b# @sk@ ^nn^

    private String title; // #v100# ^nn^

    private String content; // #v500# ^n^

    private Map<String, String> extra = Maps.newHashMap(); // #m# ^n^

    private int type; // #t# ^nn^

    private long uid; // #b# ^nn 0^

    private String uids; // #m# ^n^

    private int platform;

    private long time; // #b# ^nn^

    private int status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getUids() {
        return uids;
    }

    public void setUids(String uids) {
        this.uids = uids;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PushMsg{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", extra=" + extra +
                ", type=" + type +
                ", uid=" + uid +
                ", uids='" + uids + '\'' +
                ", platform=" + platform +
                ", time=" + time +
                ", status=" + status +
                '}';
    }
}
