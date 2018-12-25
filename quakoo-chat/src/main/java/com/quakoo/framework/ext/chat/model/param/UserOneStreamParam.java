package com.quakoo.framework.ext.chat.model.param;

import java.util.List;

import com.quakoo.framework.ext.chat.model.UserStream;

public class UserOneStreamParam {
	
	private long uid;
	
	private long type;
	
	private long thirdId;
		
    private double index;

    private int count;
		
    private List<UserStream> dataList;

	public UserOneStreamParam() {
		super();
	}

	public UserOneStreamParam(long uid, long type, long thirdId, double index) {
		super();
		this.uid = uid;
		this.type = type;
		this.thirdId = thirdId;
		this.index = index;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public long getType() {
		return type;
	}

	public void setType(long type) {
		this.type = type;
	}

	public long getThirdId() {
		return thirdId;
	}

	public void setThirdId(long thirdId) {
		this.thirdId = thirdId;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
	public String toString() {
		return "UserOneStreamParam [uid=" + uid + ", type=" + type
				+ ", thirdId=" + thirdId + ", index=" + index + ", "
				+ (dataList != null ? "dataList=" + dataList : "") + "]";
	}


}
