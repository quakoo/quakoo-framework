package com.quakoo.framework.ext.push.model;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;

/**
 * 推送所有用户的消息队列
 * class_name: PushMsgHandleAllQueue
 * package: com.quakoo.framework.ext.push.model
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 12:01
 **/
public class PushMsgHandleAllQueue implements Serializable {

    private long id;

    private long pushMsgId;

    private String title;

    private String content;

    private Map<String, String> extra = Maps.newHashMap();

    private int platform;

    private long time;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPushMsgId() {
        return pushMsgId;
    }

    public void setPushMsgId(long pushMsgId) {
        this.pushMsgId = pushMsgId;
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
