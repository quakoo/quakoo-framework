package com.quakoo.framework.ext.push.model;

import java.io.Serializable;

/**
 * 推送用户队列的信息
 * class_name: PushUserQueueInfo
 * package: com.quakoo.framework.ext.push.model
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 12:03
 **/
public class PushUserQueueInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final int end_yes = 1;
	public static final int end_no = 0;
	
	public String tableName; // #v50# @hk@ ^nn^
	
	private long phaqid; // #b# ^nn 0^
	
	private long index; // #b# ^nn 0^
	
	private int end; // #t# ^nn 0^

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public long getPhaqid() {
		return phaqid;
	}

	public void setPhaqid(long phaqid) {
		this.phaqid = phaqid;
	}

	public long getIndex() {
		return index;
	}

	public void setIndex(long index) {
		this.index = index;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}
	
}
