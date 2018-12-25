package com.quakoo.framework.ext.chat.model.back;

import java.util.List;


public class StreamBack {

	private long uid;
	
	private int type;
	
	private long thirdId;
	
	private String thirdNick;
	
	private String thirdIcon;
	
	private double maxIndex;
	
	private List<MessageBack> data;
	
	private boolean more;

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

	public String getThirdNick() {
		return thirdNick;
	}

	public void setThirdNick(String thirdNick) {
		this.thirdNick = thirdNick;
	}

	public String getThirdIcon() {
		return thirdIcon;
	}

	public void setThirdIcon(String thirdIcon) {
		this.thirdIcon = thirdIcon;
	}

	public double getMaxIndex() {
		return maxIndex;
	}

	public void setMaxIndex(double maxIndex) {
		this.maxIndex = maxIndex;
	}

	public List<MessageBack> getData() {
		return data;
	}

	public void setData(List<MessageBack> data) {
		this.data = data;
	}

	public boolean isMore() {
		return more;
	}

	public void setMore(boolean more) {
		this.more = more;
	}

	@Override
	public String toString() {
		return "StreamBack [uid=" + uid + ", type=" + type + ", thirdId="
				+ thirdId + ", "
				+ (thirdNick != null ? "thirdNick=" + thirdNick + ", " : "")
				+ (thirdIcon != null ? "thirdIcon=" + thirdIcon + ", " : "")
				+ "maxIndex=" + maxIndex + ", "
				+ (data != null ? "data=" + data + ", " : "") + "more=" + more
				+ "]";
	}
	
}
