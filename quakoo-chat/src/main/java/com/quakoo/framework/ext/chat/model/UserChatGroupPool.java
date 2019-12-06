package com.quakoo.framework.ext.chat.model;


import java.io.Serializable;

public class UserChatGroupPool implements Serializable {

    public final static int type_group = 1;//群主
    public final static int type_normal = 2;//普通

    private long uid;// #b# @hk@ ^nn^

    private long cgid;// #b# @hk@ ^nn^


    private int type;// #i# ^nn 0^

    private int status;

    private long inviteUid;

    private String ext;

    private long ctime;

    private long utime;

    @Override
    public String toString() {
        return "UserChatGroupPool{" +
                "uid=" + uid +
                ", cgid=" + cgid +
                ", type=" + type +
                ", status=" + status +
                ", inviteUid=" + inviteUid +
                ", ext='" + ext + '\'' +
                ", ctime=" + ctime +
                ", utime=" + utime +
                '}';
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getCgid() {
        return cgid;
    }

    public void setCgid(long cgid) {
        this.cgid = cgid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getUtime() {
        return utime;
    }

    public void setUtime(long utime) {
        this.utime = utime;
    }

    public long getInviteUid() {
        return inviteUid;
    }

    public void setInviteUid(long inviteUid) {
        this.inviteUid = inviteUid;
    }
}
