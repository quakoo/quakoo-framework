package com.quakoo.framework.ext.chat.model.param.nio;

public class CheckResponse extends SessionResponse {

	public CheckResponse() {
		super();
		this.type = type_check;
	}

    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
