package com.quakoo.space.interfaces;

import java.io.Serializable;

public class HNoIdDomain implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6579321929974323714L;

    protected long ctime;

    protected long utime;

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
