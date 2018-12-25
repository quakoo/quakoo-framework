package com.quakoo.framework.ext.chat.model.param.nio;

public class ChatRequest extends SessionRequest {

	private long uid;
	private String clientId;
	private int chatType;
	private long thirdId;
	private String word;
	private String voice;
	private String voiceDuration;
	private String video;
	private String videoDuration;
	private String picture;
	private String ext;

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public long getUid() {
		return uid;
	}
	
	public void setUid(long uid) {
		this.uid = uid;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public long getThirdId() {
		return thirdId;
	}
	
	public void setThirdId(long thirdId) {
		this.thirdId = thirdId;
	}
	
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	
	public String getVoice() {
		return voice;
	}
	
	public void setVoice(String voice) {
		this.voice = voice;
	}
	
	public String getVoiceDuration() {
		return voiceDuration;
	}
	
	public void setVoiceDuration(String voiceDuration) {
		this.voiceDuration = voiceDuration;
	}
	
	public String getPicture() {
		return picture;
	}
	
	public void setPicture(String picture) {
		this.picture = picture;
	}

	public int getChatType() {
		return chatType;
	}

	public void setChatType(int chatType) {
		this.chatType = chatType;
	}

	public String getVideo() {
		return video;
	}

	public void setVideo(String video) {
		this.video = video;
	}

	public String getVideoDuration() {
		return videoDuration;
	}

	public void setVideoDuration(String videoDuration) {
		this.videoDuration = videoDuration;
	}
	
}
