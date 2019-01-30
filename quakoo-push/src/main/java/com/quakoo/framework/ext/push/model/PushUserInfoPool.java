package com.quakoo.framework.ext.push.model;

import java.io.Serializable;

/**
 * 推送用户信息
 * class_name: PushUserInfoPool
 * package: com.quakoo.framework.ext.push.model
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 12:02
 **/
public class PushUserInfoPool implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private long uid; // #b# @hk@ ^nn^
	
	private int platform; // #t# @hk@ ^nn^
	
	private int brand; // #t# @hk@ ^nn^
	
	private String sessionId; // #v200# @hk@ ^nn^
	
	private String iosToken; // #v100# ^n^

    private String huaWeiToken;

    private String meiZuPushId;
	
	private long activeTime; // #b# ^nn 0^

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

	public long getActiveTime() {
		return activeTime;
	}

	public void setActiveTime(long activeTime) {
		this.activeTime = activeTime;
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

    @Override
    public String toString() {
        return "PushUserInfoPool{" +
                "uid=" + uid +
                ", platform=" + platform +
                ", brand=" + brand +
                ", sessionId='" + sessionId + '\'' +
                ", iosToken='" + iosToken + '\'' +
                ", huaWeiToken='" + huaWeiToken + '\'' +
                ", meiZuPushId='" + meiZuPushId + '\'' +
                ", activeTime=" + activeTime +
                '}';
    }
}
