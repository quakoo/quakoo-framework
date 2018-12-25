package com.quakoo.framework.ext.chat.model;

public class MessageNotice {

	private String title;
	
	private String desc;
	
	private String cover;
	
	private String redirect;
	
	public MessageNotice() {
		super();
	}

	public MessageNotice(String title, String desc, String cover,
			String redirect) {
		super();
		this.title = title;
		this.desc = desc;
		this.cover = cover;
		this.redirect = redirect;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}

	public String getRedirect() {
		return redirect;
	}

	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}

	@Override
	public String toString() {
		return "MessageNotice ["
				+ (title != null ? "title=" + title + ", " : "")
				+ (desc != null ? "desc=" + desc + ", " : "")
				+ (cover != null ? "cover=" + cover + ", " : "")
				+ (redirect != null ? "redirect=" + redirect : "") + "]";
	}
	
}
