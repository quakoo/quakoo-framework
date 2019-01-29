package com.quakoo.framework.ext.chat.model.back;

import java.util.List;

/**
 * 连接消息返回类
 * class_name: ConnectBack
 * package: com.quakoo.framework.ext.chat.model.back
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:05
 **/
public class ConnectBack {

    private double maxStreamIndex; //最大的消息游标
	
	private List<StreamBack> streams; //信息流
	
	private List<PromptBack> prompts;
	
	private boolean send; //是否发送

	public List<StreamBack> getStreams() {
		return streams;
	}

	public void setStreams(List<StreamBack> streams) {
		this.streams = streams;
	}

	public List<PromptBack> getPrompts() {
		return prompts;
	}

	public void setPrompts(List<PromptBack> prompts) {
		this.prompts = prompts;
	}

	public double getMaxStreamIndex() {
		return maxStreamIndex;
	}

	public void setMaxStreamIndex(double maxStreamIndex) {
		this.maxStreamIndex = maxStreamIndex;
	}

	public boolean isSend() {
		return send;
	}

	public void setSend(boolean send) {
		this.send = send;
	}

	@Override
	public String toString() {
		return "ConnectBack [maxStreamIndex=" + maxStreamIndex + ", "
				+ (streams != null ? "streams=" + streams + ", " : "")
				+ (prompts != null ? "prompts=" + prompts + ", " : "")
				+ "send=" + send + "]";
	}
	
}
