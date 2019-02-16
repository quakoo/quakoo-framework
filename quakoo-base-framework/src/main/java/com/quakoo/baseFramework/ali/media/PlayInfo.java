package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class PlayInfo implements Serializable {
//    Bitrate	String	视频流码率，单位Kbps
//    Definition	String	视频流清晰度定义, 取值：FD(流畅)，LD(标清)，SD(高清)，HD(超清)，OD(原画)，2K(2K)，4K(4K)
//    Duration	String	视频流长度，单位秒
//    Encrypt	Long	视频流是否加密流，取值：0(否)，1(是)
//    PlayURL	String	视频流的播放地址
//    Format	String	视频流格式，若媒体文件为视频则取值：mp4, m3u8，若是纯音频则取值：mp3
//    Fps	String	视频流帧率，每秒多少帧
//    Size	Long	视频流大小，单位Byte
//    Width	Long	视频流宽度，单位px
//    Height	Long	视频流高度，单位px
//    StreamType	String	视频流类型，若媒体流为视频则取值：video，若是纯音频则取值：audio
//    JobId	String	媒体流转码的作业ID，作为媒体流的唯一标识


	@JsonProperty("Bitrate")  
    private String bitrate;//	String	视频流码率，单位Kbps
	@JsonProperty("Definition")  
    private String definition;//	String	视频流清晰度定义, 取值：FD(流畅)，LD(标清)，SD(高清)，HD(超清)，OD(原画)，2K(2K)，4K(4K)

	@JsonProperty("Duration")  
	private String duration;//	String	视频流长度，单位秒
	@JsonProperty("Encrypt")  
    private Long encrypt;//	Long	视频流是否加密流，取值：0(否)，1(是)
	@JsonProperty("PlayURL")  
    private String playURL;//	String	视频流的播放地址
	@JsonProperty("Format")  
    private String format;//	String	视频流格式，若媒体文件为视频则取值：mp4, m3u8，若是纯音频则取值：mp3
	@JsonProperty("Fps")  
    private String fps;//	String	视频流帧率，每秒多少帧
	@JsonProperty("Size")  
    private Long size;//	Long	视频流大小，单位Byte
	@JsonProperty("Width")  
    private Long width;//	Long	视频流宽度，单位px
	@JsonProperty("Height")  
    private Long  height;//	Long	视频流高度，单位px
	@JsonProperty("StreamType")  
    private String streamType;//	String	视频流类型，若媒体流为视频则取值：video，若是纯音频则取值：audio
	@JsonProperty("JobId")  
    private String jobId;//	String	媒体流转码的作业ID，作为媒体流的唯一标识
	public String getBitrate() {
		return bitrate;
	}
	public void setBitrate(String bitrate) {
		this.bitrate = bitrate;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public Long getEncrypt() {
		return encrypt;
	}
	public void setEncrypt(Long encrypt) {
		this.encrypt = encrypt;
	}
	public String getPlayURL() {
		return playURL;
	}
	public void setPlayURL(String playURL) {
		this.playURL = playURL;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public String getFps() {
		return fps;
	}
	public void setFps(String fps) {
		this.fps = fps;
	}
	public Long getSize() {
		return size;
	}
	public void setSize(Long size) {
		this.size = size;
	}
	public Long getWidth() {
		return width;
	}
	public void setWidth(Long width) {
		this.width = width;
	}
	public Long getHeight() {
		return height;
	}
	public void setHeight(Long height) {
		this.height = height;
	}
	public String getStreamType() {
		return streamType;
	}
	public void setStreamType(String streamType) {
		this.streamType = streamType;
	}
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	
}
