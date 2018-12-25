package com.quakoo.framework.ext.chat.model.back;

public class UserBack {
	
	private long id;
	
	private String nick;
	
	private String icon;


	
	public UserBack() {
		super();
	}

	public UserBack(long id, String nick, String icon) {
		super();
		this.id = id;
		this.nick = nick;
		this.icon = icon;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Override
	public String toString() {
		return "UserBack [id=" + id + ", "
				+ (nick != null ? "nick=" + nick + ", " : "")
				+ (icon != null ? "icon=" + icon : "") + "]";
	}
	
}
