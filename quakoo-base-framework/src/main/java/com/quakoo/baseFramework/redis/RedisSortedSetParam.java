package com.quakoo.baseFramework.redis;

public class RedisSortedSetParam {

	private String key;
    private double minScore;
    private double maxScore;
    private int offset;
    private int count;
    private boolean isasc;
    
    private Object attach;
    
    
	public RedisSortedSetParam(String key, double minScore, double maxScore,
			int offset, int count, boolean isasc) {
		super();
		this.key = key;
		this.minScore = minScore;
		this.maxScore = maxScore;
		this.offset = offset;
		this.count = count;
		this.isasc = isasc;
	}
	
	
	public RedisSortedSetParam() {
		super();
	}


	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public double getMinScore() {
		return minScore;
	}
	public void setMinScore(double minScore) {
		this.minScore = minScore;
	}
	public double getMaxScore() {
		return maxScore;
	}
	public void setMaxScore(double maxScore) {
		this.maxScore = maxScore;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public boolean isIsasc() {
		return isasc;
	}
	public void setIsasc(boolean isasc) {
		this.isasc = isasc;
	}
	public Object getAttach() {
		return attach;
	}
	public void setAttach(Object attach) {
		this.attach = attach;
	}
	@Override
	public String toString() {
		return "RedisSortedSetParam ["
				+ (key != null ? "key=" + key + ", " : "") + "minScore="
				+ minScore + ", maxScore=" + maxScore + ", offset=" + offset
				+ ", count=" + count + ", isasc=" + isasc + ", "
				+ (attach != null ? "attach=" + attach : "") + "]";
	}
	
    
}
