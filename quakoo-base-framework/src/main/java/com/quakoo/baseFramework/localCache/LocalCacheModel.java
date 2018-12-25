package com.quakoo.baseFramework.localCache;

public class LocalCacheModel {

	private Object value;

	private long overtime;

	public LocalCacheModel(Object value, long overtime) {
		super();
		this.value = value;
		this.overtime = overtime;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public long getOvertime() {
		return overtime;
	}

	public void setOvertime(long overtime) {
		this.overtime = overtime;
	}

}
