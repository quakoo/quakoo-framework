package com.quakoo.framework.ext.chat.model.ext;

import java.io.Serializable;

public class ChatCheckRes implements Serializable {

    private boolean success;

    private String msg;

    public ChatCheckRes() {
    }

    public ChatCheckRes(boolean success, String msg) {
        this.success = success;
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
