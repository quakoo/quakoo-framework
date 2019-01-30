package com.quakoo.framework.ext.push.model;

import java.io.Serializable;

/**
 * 推送用户队列
 * class_name: PushUserQueue
 * package: com.quakoo.framework.ext.push.model
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 12:03
 **/
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
