package com.quakoo.framework.ext.chat.model.param.nio;

public class ChatResponse extends SessionResponse {

	public ChatResponse() {
		super();
		this.type = type_chat;
	}

    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
