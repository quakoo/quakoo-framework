package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 911432745995026601L;
	
    private long id;
	
	private long authorId;
	
	private String clientId;
	
	private int type;
	
	private String content;

	private long time;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(long authorId) {
		this.authorId = authorId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
