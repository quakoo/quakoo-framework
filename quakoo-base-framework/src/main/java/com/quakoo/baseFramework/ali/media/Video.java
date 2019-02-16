package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Video implements Serializable {

//    VideoId	String	视频ID
//    Title	String	视频标题
//    Description	String	视频描述
//    Duration	Float	视频时长(秒)
//    CoverURL	String	视频封面URL
//    Status	String	视频状态
//    CreationTime	String	视频创建时间，为UTC时间
//    Size	Long	视频源文件大小(字节)
//    Snapshots	String[]	视频截图URL数组
//    CateId	Long	视频分类ID
//    CateName	String	视频分类名称
//    Tags	String	视频标签，多个会用逗号分隔
//    playerUrl 播放地址

//    视频状态
//    Uploading	上传中	视频的初始状态，表示正在上传
//    UploadFail	上传失败	由于是断点续传，无法确定上传是否失败，故暂不会出现此值
//    UploadSucc	上传完成
//    Transcoding	转码中
//    TranscodeFail	转码失败	转码失败，一般是原片有问题，可在事件通知的转码完成消息得到ErrorMessage失败信息，或提交工单联系我们
//    Checking	审核中	在“视频点播控制台-全局设置-审核设置”开启了“先审后发”，转码成功后视频状态会变成审核中，此时视频只能在控制台播放
//    Blocked	屏蔽	在审核时屏蔽视频
//    Normal	正常	视频可正常播放


	@JsonProperty("VideoId")  
    private String videoId;//	String	视频ID
	@JsonProperty("Title")  
    private String title;//	String	视频标题
	@JsonProperty("Description")  
    private String description;//	String	视频描述
	@JsonProperty("Duration")  
    private double duration;//	Float	视频时长(秒)
	@JsonProperty("CoverURL")  
    private String coverURL;//	String	视频封面URL
	@JsonProperty("Status")  
    private String status;//		String	视频状态
	@JsonProperty("CreationTime")  
    private String creationTime;//		String	视频创建时间，为UTC时间
	@JsonProperty("Size")  
    private long size;//		Long	视频源文件大小(字节)
//	@JsonProperty("Snapshots")  
//    private List<String> Snapshots;//		String[]	视频截图URL数组
	@JsonProperty("CateId")  
    private long cateId;//		Long	视频分类ID
	@JsonProperty("CateName")  
    private String cateName;//		String	视频分类名称
	@JsonProperty("Tags")  
    private String tags;//		String	视频标签，多个会用逗号分隔
    private List<PlayInfo> playInfos=new ArrayList<>();//  视频播放地址
	public String getVideoId() {
		return videoId;
	}
	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public double getDuration() {
		return duration;
	}
	public void setDuration(double duration) {
		this.duration = duration;
	}
	public String getCoverURL() {
		return coverURL;
	}
	public void setCoverURL(String coverURL) {
		this.coverURL = coverURL;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(String creationTime) {
		this.creationTime = creationTime;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public long getCateId() {
		return cateId;
	}
	public void setCateId(long cateId) {
		this.cateId = cateId;
	}
	public String getCateName() {
		return cateName;
	}
	public void setCateName(String cateName) {
		this.cateName = cateName;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}
	public List<PlayInfo> getPlayInfos() {
		return playInfos;
	}
	public void setPlayInfos(List<PlayInfo> playInfos) {
		this.playInfos = playInfos;
	}

   
}
