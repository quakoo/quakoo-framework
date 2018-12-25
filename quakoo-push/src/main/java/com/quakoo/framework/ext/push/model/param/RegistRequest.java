package com.quakoo.framework.ext.push.model.param;

public class RegistRequest extends SessionRequest {

    private long uid; 
	
	private int platform;
	
	private int brand;
	
	private String phoneSessionId;
	
	private String iosToken;

	private String huaWeiToken;

	private String meiZuPushId;

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
}
