package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Result {

    private int Code;
	
	private String Message;
	@JsonProperty("RequestId")  
	private String RequestId;

	@JsonProperty("LiveRecordVideoList")    
	private LiveRecordVideoList LiveRecordVideoList;

	@JsonProperty("PlayInfoList")
	private PlayInfoList PlayInfoList;

	@JsonProperty("Mezzanine")
	private Mezzanine mezzanine;

	public int getCode() {
		return Code;
	}

	public void setCode(int code) {
		this.Code = Code;
	}


	public String getMessage() {
		return Message;
	}

	public void setMessage(String message) {
		Message = message;
	}

	public String getRequestId() {
		return RequestId;
	}

	public void setRequestId(String requestId) {
		RequestId = requestId;
	}


    public Mezzanine getMezzanine() {
        return mezzanine;
    }

    public void setMezzanine(Mezzanine mezzanine) {
        this.mezzanine = mezzanine;
    }

    public LiveRecordVideoList getLiveRecordVideoList() {
		return LiveRecordVideoList;
	}

	public void setLiveRecordVideoList(LiveRecordVideoList liveRecordVideoList) {
		LiveRecordVideoList = liveRecordVideoList;
	}

	public PlayInfoList getPlayInfoList() {
		return PlayInfoList;
	}

	public void setPlayInfoList(PlayInfoList playInfoList) {
		PlayInfoList = playInfoList;
	}

	
}
