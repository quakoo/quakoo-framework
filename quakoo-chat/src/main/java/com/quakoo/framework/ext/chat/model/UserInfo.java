package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class UserInfo implements Serializable{

	private static final long serialVersionUID = 8098284495555974593L;

	private long uid; //用户ID
	
	private double lastIndex; //上次消息游标
	
	private double promptIndex; //提示游标
	
	private double loginTime; //登陆时间

    private long persistentTime;

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

    @Override
	public String toString() {
		return "UserInfo [uid=" + uid + ", lastIndex=" + lastIndex
				+ ", promptIndex=" + promptIndex + ", loginTime=" + loginTime
				+ "]";
	}

}
