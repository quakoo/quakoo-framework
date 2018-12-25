package com.quakoo.framework.ext.push.model.param;


public class NioUserLongConnection {

	private long activeTime;

    private long uid; 
	
	private int platform; 
	
	private int brand;
	
	private String sessionId;
	
	public NioUserLongConnection() {
		super();
	}
	
	public NioUserLongConnection(long uid, int platform, 
			int brand, String sessionId, long activeTime) {
		super();
		this.activeTime = activeTime;
		this.uid = uid;
		this.platform = platform;
		this.brand = brand;
		this.sessionId = sessionId;
	}

	public long getActiveTime() {
		return activeTime;
	}

	public void setActiveTime(long activeTime) {
		this.activeTime = activeTime;
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

	@Override
	public String toString() {
		return "NioUserLongConnection [activeTime=" + activeTime + ", uid="
				+ uid + ", platform=" + platform + ", brand=" + brand + ", "
				+ (sessionId != null ? "sessionId=" + sessionId : "") + "]";
	}
	
}
