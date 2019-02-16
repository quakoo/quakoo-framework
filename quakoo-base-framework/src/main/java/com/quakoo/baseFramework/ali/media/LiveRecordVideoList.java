package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LiveRecordVideoList {

	@JsonProperty("LiveRecordVideo")  
	private List<LiveRecordVideo> liveRecordVideo;

	public List<LiveRecordVideo> getLiveRecordVideo() {
		return liveRecordVideo;
	}

	public void setLiveRecordVideo(List<LiveRecordVideo> liveRecordVideo) {
		this.liveRecordVideo = liveRecordVideo;
	}

	

	
}
