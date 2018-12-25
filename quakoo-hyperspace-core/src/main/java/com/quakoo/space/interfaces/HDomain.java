package com.quakoo.space.interfaces;

import java.io.Serializable;

import com.quakoo.space.annotation.domain.PrimaryKey;

public class HDomain implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 383434754586538159L;

    @PrimaryKey
    protected long id;

    protected long ctime;

    protected long utime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

}
