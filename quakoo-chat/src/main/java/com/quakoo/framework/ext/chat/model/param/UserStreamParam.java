package com.quakoo.framework.ext.chat.model.param;

import java.util.List;

import com.quakoo.framework.ext.chat.model.UserStream;


public class UserStreamParam {
	
    private long uid;
	
	private double index;
	
	private List<UserStream> dataList;
	
	public UserStreamParam() {
		super();
	}

	public UserStreamParam(long uid, double index) {
		super();
		this.uid = uid;
		this.index = index;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public double getIndex() {
		return index;
	}

	public void setIndex(double index) {
		this.index = index;
	}

	public List<UserStream> getDataList() {
		return dataList;
	}

	public void setDataList(List<UserStream> dataList) {
		this.dataList = dataList;
	}

	@Override
	public String toString() {
		return "UserStreamParam [uid=" + uid + ", index=" + index + ", "
				+ (dataList != null ? "dataList=" + dataList : "") + "]";
	}


}
