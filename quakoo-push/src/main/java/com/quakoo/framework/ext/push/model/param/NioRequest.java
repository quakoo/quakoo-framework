package com.quakoo.framework.ext.push.model.param;

public class NioRequest {

	public static final int type_regist = 1;
	public static final int type_ping = 2;
	public static final int type_ios_clear_badge = 3;
	public static final int type_logout = 5;
	
	protected int type;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "NioRequest [type=" + type + "]";
	}
	
}
