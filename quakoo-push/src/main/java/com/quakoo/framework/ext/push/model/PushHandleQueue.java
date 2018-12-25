package com.quakoo.framework.ext.push.model;

import java.io.Serializable;

public class PushHandleQueue implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final int type_single = 1;
	public static final int type_batch = 2;
	
	private long id; // #b# @ak@ ^nn^
	
	private int shardNum; // #i# ^nn^
	
	private int type; // #t# ^nn^
	
	private long uid; // #b# ^nn 0^
 	
	private String uids; // #m# ^n^
	
	private long payloadId; // #b# ^nn^
	
	private long time; // #b# ^nn^

    public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getShardNum() {
		return shardNum;
	}

	public void setShardNum(int shardNum) {
		this.shardNum = shardNum;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public String getUids() {
		return uids;
	}

	public void setUids(String uids) {
		this.uids = uids;
	}

	public long getPayloadId() {
		return payloadId;
	}

	public void setPayloadId(long payloadId) {
		this.payloadId = payloadId;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "PushHandleQueue [id=" + id + ", shardNum=" + shardNum
				+ ", type=" + type + ", uid=" + uid + ", "
				+ (uids != null ? "uids=" + uids + ", " : "") + "payloadId="
				+ payloadId + ", time=" + time + "]";
	}
	
}
