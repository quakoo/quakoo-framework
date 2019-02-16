package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LiveRecordVideo implements java.io.Serializable {
//    StreamName	String	直播流名称
//    DomainName	String	域名
//    AppName	String	App名称
//    PlaylistId	String	播单ID
//    RecordStartTime	String	录制开始时间
//    RecordEndTime	String	录制结束时间
//    Video	Video	视频信息


	@JsonProperty("StreamName")  
    private String  streamName;//		直播流名称
	@JsonProperty("DomainName")  
    private String    domainName;//	String	域名
	@JsonProperty("AppName")  
    private String    appName;//	String	App名称
	@JsonProperty("PlaylistId")  
    private String    playlistId;//	String	播单ID
	@JsonProperty("RecordStartTime")  
    private String    recordStartTime;//	String	录制开始时间
	@JsonProperty("RecordEndTime")  
    private String   recordEndTime;//	String	录制结束时间
	@JsonProperty("Video")  
    private Video video;//	Video	视频信息
	public String getStreamName() {
		return streamName;
	}
	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}
	public String getDomainName() {
		return domainName;
	}
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getPlaylistId() {
		return playlistId;
	}
	public void setPlaylistId(String playlistId) {
		this.playlistId = playlistId;
	}
	public String getRecordStartTime() {
		return recordStartTime;
	}
	public void setRecordStartTime(String recordStartTime) {
		this.recordStartTime = recordStartTime;
	}
	public String getRecordEndTime() {
		return recordEndTime;
	}
	public void setRecordEndTime(String recordEndTime) {
		this.recordEndTime = recordEndTime;
	}
	public Video getVideo() {
		return video;
	}
	public void setVideo(Video video) {
		this.video = video;
	}

  
}
