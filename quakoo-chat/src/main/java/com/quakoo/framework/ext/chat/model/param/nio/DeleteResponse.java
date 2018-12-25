package com.quakoo.framework.ext.chat.model.param.nio;

public class DeleteResponse extends SessionResponse {

	public DeleteResponse() {
		super();
		this.type = type_delete;
	}

    private long mid;

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
    }

}
