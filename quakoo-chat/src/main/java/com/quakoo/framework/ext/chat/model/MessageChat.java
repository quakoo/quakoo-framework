package com.quakoo.framework.ext.chat.model;

public class MessageChat {

	private String word;
	
	private String picture;
	
	private String voice;
	
	private String voiceDuration;
	
	private String video;
	
	private String videoDuration;

	private String ext;

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public MessageChat() {
		super();
	}

	public MessageChat(String ext) {
        super();
        this.ext = ext;
    }

	public MessageChat(String word, String picture, String voice, String voiceDuration,
			String video, String videoDuration, String ext) {
		super();
		this.word = word;
		this.picture = picture;
		this.voice = voice;
		this.voiceDuration = voiceDuration;
		this.video = video;
		this.videoDuration = videoDuration;

		this.ext = ext;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
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

    @Override
    public String toString() {
        return "MessageChat{" +
                "word='" + word + '\'' +
                ", picture='" + picture + '\'' +
                ", voice='" + voice + '\'' +
                ", voiceDuration='" + voiceDuration + '\'' +
                ", video='" + video + '\'' +
                ", videoDuration='" + videoDuration + '\'' +
                ", ext='" + ext + '\'' +
                '}';
    }
}
