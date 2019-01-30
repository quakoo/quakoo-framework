package com.quakoo.framework.ext.push.bean;

import java.io.Serializable;

/**
 * 异步推送用户信息
 * class_name: PushUserInfoMsg
 * package: com.quakoo.framework.ext.push.bean
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:02
 **/
public class PushUserInfoMsg implements Serializable {

    public static final int type_regist = 1;
    public static final int type_logout = 2;

    private int type;

    private long uid;
    private int platform;
    private int brand;
    private String sessionId;
    private String iosToken;
    private String huaWeiToken;
    private String meiZuPushId;

    private long time;

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

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public int getBrand() {
        return brand;
    }

    public void setBrand(int brand) {
        this.brand = brand;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIosToken() {
        return iosToken;
    }

    public void setIosToken(String iosToken) {
        this.iosToken = iosToken;
    }

    public String getHuaWeiToken() {
        return huaWeiToken;
    }

    public void setHuaWeiToken(String huaWeiToken) {
        this.huaWeiToken = huaWeiToken;
    }

    public String getMeiZuPushId() {
        return meiZuPushId;
    }

    public void setMeiZuPushId(String meiZuPushId) {
        this.meiZuPushId = meiZuPushId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
