package com.quakoo.baseFramework.model.pagination.service;


public class RequestOwner {


	private Class clazz;
	private int index;
	public Class getClazz() {
		return clazz;
	}
	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public RequestOwner(Class clazz, int index) {
		super();
		this.clazz = clazz;
		this.index = index;
	}
	public RequestOwner() {
		super();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + index;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RequestOwner other = (RequestOwner) obj;
		if (clazz == null) {
			if (other.clazz != null) {
				return false;
			}
		} else if (!clazz.equals(other.clazz)) {
			return false;
		}
		if (index != other.index) {
			return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return "RequestOwner [clazz=" + clazz + ", index=" + index + "]";
	}



}
