package com.quakoo.framework.ext.push.model.param;


public class ErrorResponse extends NioResponse {

	private String msg;
	
	public ErrorResponse() {
		super();
		super.type = type_error;
		super.success = false;
	}

	public ErrorResponse(String msg) {
		this();
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "ErrorResponse [" + (msg != null ? "msg=" + msg + ", " : "")
				+ "success=" + success + "]";
	}

}
