package com.quakoo.framework.ext.chat.model.param.nio;


public class DeleteRequest extends SessionRequest {
	
	private long uid;
	
	private int chatType;
	
	private long thirdId;
	
	private long mid;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public int getChatType() {
		return chatType;
	}

	public void setChatType(int chatType) {
		this.chatType = chatType;
	}

	public long getThirdId() {
		return thirdId;
	}

	public void setThirdId(long thirdId) {
		this.thirdId = thirdId;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}
	
}
