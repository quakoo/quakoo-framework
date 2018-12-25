package com.quakoo.framework.ext.chat.model.ext;

import com.quakoo.framework.ext.chat.model.MessageChat;

import java.io.Serializable;

public class OtherChatRes implements Serializable {

    private boolean success;

    private MessageChat messageChat;

    private String errMsg;

    public OtherChatRes() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public MessageChat getMessageChat() {
        return messageChat;
    }

    public void setMessageChat(MessageChat messageChat) {
        this.messageChat = messageChat;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}
