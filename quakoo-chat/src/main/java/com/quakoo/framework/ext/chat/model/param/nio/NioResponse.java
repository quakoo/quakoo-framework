package com.quakoo.framework.ext.chat.model.param.nio;

public class NioResponse {
	
	public static final int type_error = 0;
	public static final int type_pong = 1;
	public static final int type_chat = 2;
	public static final int type_delete = 3;
	public static final int type_check = 4;
	public static final int type_connect = 5;
	public static final int type_recall = 6;

	public static final int type_againlogin = 7;

    public static final int type_other_chat = 10;
	
	protected int type;
	
	protected boolean success;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
