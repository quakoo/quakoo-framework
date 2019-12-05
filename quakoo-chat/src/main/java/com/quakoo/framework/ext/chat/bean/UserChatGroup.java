package com.quakoo.framework.ext.chat.bean;

import java.io.Serializable;

public class UserChatGroup implements Serializable {

    private long uid;

    private long cgid;

    public long getCgid() {
        return cgid;
    }

    public void setCgid(long cgid) {
        this.cgid = cgid;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }
}
