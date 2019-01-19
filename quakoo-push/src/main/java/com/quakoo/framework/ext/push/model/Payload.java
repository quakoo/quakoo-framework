//package com.quakoo.framework.ext.push.model;
//
//import java.io.Serializable;
//import java.util.Map;
//
//import com.google.common.collect.Maps;
//import com.quakoo.baseFramework.jackson.JsonUtils;
//
//public class Payload implements Serializable {
//
//	private static final long serialVersionUID = 1L;
//
//	private long id; // #b# @sk@ ^nn^
//
//	private String title; // #v100# ^nn^
//
//	private String content; // #v500# ^n^
//
//	private Map<String, String> extra = Maps.newHashMap(); // #m# ^n^
//
//	private long time; // #b# ^nn^
//
//    private int platform;
//
//	public static void main(String[] args) {
//		Payload p = new Payload();
//		p.setId(1);
//		String str = JsonUtils.toJson(p);
//		p.setExtra(null);
//		System.out.println(str);
//	}
//
//	public long getId() {
//		return id;
//	}
//
//	public void setId(long id) {
//		this.id = id;
//	}
//
//	public String getTitle() {
//		return title;
//	}
//
//	public void setTitle(String title) {
//		this.title = title;
//	}
//
//	public String getContent() {
//		return content;
//	}
//
//	public void setContent(String content) {
//		this.content = content;
//	}
//
//	public Map<String, String> getExtra() {
//		return extra;
//	}
//
//	public void setExtra(Map<String, String> extra) {
//		this.extra = extra;
//	}
//
//	public long getTime() {
//		return time;
//	}
//
//	public void setTime(long time) {
//		this.time = time;
//	}
//
//    public int getPlatform() {
//        return platform;
//    }
//
//    public void setPlatform(int platform) {
//        this.platform = platform;
//    }
//
//    @Override
//	public String toString() {
//		return "Payload [id=" + id + ", "
//				+ (title != null ? "title=" + title + ", " : "")
//				+ (content != null ? "content=" + content + ", " : "")
//				+ (extra != null ? "extra=" + extra + ", " : "") + "time="
//				+ time + "]";
//	}
//
//}
