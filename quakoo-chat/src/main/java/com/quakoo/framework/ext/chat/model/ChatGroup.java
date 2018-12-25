package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class ChatGroup implements Serializable {

	private static final long serialVersionUID = -7834771076835092126L;

	public static final int check_yes = 1;
	public static final int check_no = 0;
	
	private long id;
	
	private String name;

	private String icon;
	
	private String uids;

	private int check; //是否需要群主审核

    private String notice; //群公告

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUids() {
		return uids;
	}

	public void setUids(String uids) {
		this.uids = uids;
	}

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getCheck() {
        return check;
    }

    public void setCheck(int check) {
        this.check = check;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    @Override
	public String toString() {
		return "ChatGroup [id=" + id + ", "
				+ (name != null ? "name=" + name + ", " : "")
				+ (uids != null ? "uids=" + uids : "") + "]";
	}
	
}
