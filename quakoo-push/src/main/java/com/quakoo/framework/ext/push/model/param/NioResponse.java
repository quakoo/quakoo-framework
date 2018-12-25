package com.quakoo.framework.ext.push.model.param;

public class NioResponse {
	
	public static final int type_error = 0;
	public static final int type_regist = 1;
	public static final int type_pong = 2;
	public static final int type_ios_clear_badge = 3;
	public static final int type_payload = 4;
	public static final int type_logout = 5;
	
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
