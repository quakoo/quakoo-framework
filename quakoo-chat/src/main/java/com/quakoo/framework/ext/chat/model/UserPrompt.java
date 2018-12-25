package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

import com.quakoo.baseFramework.model.pagination.PagerCursor;

public class UserPrompt implements Serializable {

	private static final long serialVersionUID = -5359206735922351307L;
	
	private long uid;

	private int type;
	
	private long thirdId;
	
	private long num;
	
	@PagerCursor
	private double sort;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getThirdId() {
		return thirdId;
	}

	public void setThirdId(long thirdId) {
		this.thirdId = thirdId;
	}

	public long getNum() {
		return num;
	}

	public void setNum(long num) {
		this.num = num;
	}

	public double getSort() {
		return sort;
	}

	public void setSort(double sort) {
		this.sort = sort;
	}

	@Override
	public String toString() {
		return "UserPrompt [uid=" + uid + ", type=" + type + ", thirdId="
				+ thirdId + ", num=" + num + ", sort=" + sort + "]";
	}
	
}
