package com.quakoo.framework.ext.chat.model.param;

import java.io.Serializable;

public class WillPushItem implements Serializable {
	
	private static final long serialVersionUID = 2354838941762272411L;
	
    private long uid;
	
	private long mid;
	
//	private long time;
    private double sort;

	public WillPushItem() {
		super();
	}

    public WillPushItem(long uid, long mid, double sort) {
        super();
        this.uid = uid;
        this.mid = mid;
        this.sort = sort;
    }

    //	public WillPushItem(long uid, long mid, long time) {
//		super();
//		this.uid = uid;
//		this.mid = mid;
//		this.time = time;
//	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

    public double getSort() {
        return sort;
    }

    public void setSort(double sort) {
        this.sort = sort;
    }

    //	public long getTime() {
//		return time;
//	}
//
//	public void setTime(long time) {
//		this.time = time;
//	}

//	@Override
//	public String toString() {
//		return "WillPushItem [uid=" + uid + ", mid=" + mid + ", time=" + time
//				+ "]";
//	}


    @Override
    public String toString() {
        return "WillPushItem{" +
                "uid=" + uid +
                ", mid=" + mid +
                ", sort=" + sort +
                '}';
    }
}
