package com.quakoo.framework.ext.chat.model.ext;

import com.quakoo.framework.ext.chat.model.MessageChat;

import java.io.Serializable;

/**
 * 其他类型的消息结果
 * class_name: OtherChatRes
 * package: com.quakoo.framework.ext.chat.model.ext
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:11
 **/
public class OtherChatRes implements Serializable {

    private boolean success; //是否成功

    private MessageChat messageChat; //成功返回的消息体

    private String errMsg; //失败返回的原因

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
