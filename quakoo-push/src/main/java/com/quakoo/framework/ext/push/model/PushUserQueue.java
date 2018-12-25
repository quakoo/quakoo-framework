package com.quakoo.framework.ext.push.model;

import java.io.Serializable;

public class PushUserQueue implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private long index; // #b# @ak@ ^nn^
	
	private long uid; // #b# ^nn^

	public long getIndex() {
		return index;
	}

	public void setIndex(long index) {
		this.index = index;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	@Override
	public String toString() {
		return "PushUserQueue [index=" + index + ", uid=" + uid + "]";
	}
	
}
