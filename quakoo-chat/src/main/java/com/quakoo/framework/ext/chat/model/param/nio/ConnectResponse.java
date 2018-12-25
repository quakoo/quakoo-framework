package com.quakoo.framework.ext.chat.model.param.nio;

import com.quakoo.framework.ext.chat.model.back.ConnectBack;

public class ConnectResponse extends NioResponse {

	private ConnectBack result;
	
	public ConnectResponse() {
		super();
		super.type = type_connect;
		super.success = true;
	}

	public ConnectResponse(ConnectBack result) {
		this();
		super.type = type_connect;
		this.result = result;
	}

	public ConnectBack getResult() {
		return result;
	}

	public void setResult(ConnectBack result) {
		this.result = result;
	}

    @Override
    public String toString() {
        return "ConnectResponse{" +
                "result=" + result +
                '}';
    }
}
