package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PlayInfoList {

	@JsonProperty("PlayInfo")  
	private List<PlayInfo> playInfo;

	public List<PlayInfo> getPlayInfo() {
		return playInfo;
	}

	public void setPlayInfo(List<PlayInfo> playInfo) {
		this.playInfo = playInfo;
	}

	
	
	
}
