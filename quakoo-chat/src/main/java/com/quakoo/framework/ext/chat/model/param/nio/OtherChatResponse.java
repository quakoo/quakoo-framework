package com.quakoo.framework.ext.chat.model.param.nio;

public class OtherChatResponse extends SessionResponse {

    public OtherChatResponse() {
        super();
        this.type = type_other_chat;
    }

    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
