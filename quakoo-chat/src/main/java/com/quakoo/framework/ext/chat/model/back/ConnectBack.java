package com.quakoo.framework.ext.chat.model.back;

import java.util.List;


public class ConnectBack {

    private double maxStreamIndex;
	
	private List<StreamBack> streams;
	
	private List<PromptBack> prompts;
	
	private boolean send;

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
