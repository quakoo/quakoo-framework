package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class UserInfoQueue implements Serializable {

    private long uid; //用户ID

    private double lastIndex; //上次消息游标

    private double promptIndex; //提示游标

    private double loginTime; //登陆时间

    private long persistentTime;

    public UserInfoQueue() {
    }

    public UserInfoQueue(UserInfo userInfo) {
        this.uid = userInfo.getUid();
        this.lastIndex = userInfo.getLastIndex();
        this.promptIndex = userInfo.getPromptIndex();
        this.loginTime = userInfo.getLoginTime();
        this.persistentTime = userInfo.getPersistentTime();
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public double getLastIndex() {
        return lastIndex;
    }

    public void setLastIndex(double lastIndex) {
        this.lastIndex = lastIndex;
    }

    public double getPromptIndex() {
        return promptIndex;
    }

    public void setPromptIndex(double promptIndex) {
        this.promptIndex = promptIndex;
    }

    public double getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(double loginTime) {
        this.loginTime = loginTime;
    }

    public long getPersistentTime() {
        return persistentTime;
    }

    public void setPersistentTime(long persistentTime) {
        this.persistentTime = persistentTime;
    }
}
