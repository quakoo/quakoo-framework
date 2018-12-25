package com.quakoo.framework.ext.chat.model.param.nio;

public class NioRequest {

	public static final int type_ping = 1;
	public static final int type_chat = 2;
	public static final int type_delete = 3;
	public static final int type_check = 4;
	public static final int type_recall = 6;

	public static final int type_other_chat = 10;
	
	protected int type;

    protected String token;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
