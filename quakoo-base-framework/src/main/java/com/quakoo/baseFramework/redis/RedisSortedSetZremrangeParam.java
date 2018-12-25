package com.quakoo.baseFramework.redis;

public class RedisSortedSetZremrangeParam {

	private String key;
	private int start;
	private int end;
	
	
	
	public RedisSortedSetZremrangeParam() {
		super();
	}
	public RedisSortedSetZremrangeParam(String key, int start, int end) {
		super();
		this.key = key;
		this.start = start;
		this.end = end;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	@Override
	public String toString() {
		return "RedisSortedSetZremrangeParam ["
				+ (key != null ? "key=" + key + ", " : "") + "start=" + start
				+ ", end=" + end + "]";
	}
	

}
