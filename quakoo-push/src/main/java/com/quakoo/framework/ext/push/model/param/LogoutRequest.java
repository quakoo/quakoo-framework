package com.quakoo.framework.ext.push.model.param;

public class LogoutRequest extends SessionRequest {

    private long uid;

    private int platform;

    private int brand;

    private String phoneSessionId;

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

    public String getPhoneSessionId() {
        return phoneSessionId;
    }

    public void setPhoneSessionId(String phoneSessionId) {
        this.phoneSessionId = phoneSessionId;
    }

}
