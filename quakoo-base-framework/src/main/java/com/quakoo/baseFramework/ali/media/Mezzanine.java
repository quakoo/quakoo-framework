package com.quakoo.baseFramework.ali.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Mezzanine {

    @JsonProperty("FileName")
    private String fileName;

    @JsonProperty("Duration")
    private String duration;

    @JsonProperty("CreationTime")
    private String creationTime;

    @JsonProperty("FileURL")
    private String fileURL;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }
}
