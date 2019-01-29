package com.quakoo.framework.ext.chat.model.back;

import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.MessageChat;
import com.quakoo.framework.ext.chat.model.MessageNotice;
import com.quakoo.framework.ext.chat.model.constant.Type;

public class MessageBack {

	@Override
	public String toString() {
		return "MessageBack [id="
				+ id
				+ ", authorId="
				+ authorId
				+ ", "
				+ (authorNick != null ? "authorNick=" + authorNick + ", " : "")
				+ (authorIcon != null ? "authorIcon=" + authorIcon + ", " : "")
				+ (clientId != null ? "clientId=" + clientId + ", " : "")
				+ "type="
				+ type
				+ ", "
				+ (messageChat != null ? "messageChat=" + messageChat + ", "
						: "")
				+ (messageNotice != null ? "messageNotice=" + messageNotice
						+ ", " : "") + "time=" + time + ", index=" + index
				+ "]";
	}

	private long id;

	private long authorId;

	private String authorNick;

	private String authorRemark;

	private String authorIcon;

	private String clientId;
	
	private int type;
	
	private MessageChat messageChat;
	
	private MessageNotice messageNotice;

	private long time;

	private double index;
	
	public MessageBack() {
		super();
	}
	
	public MessageBack(Message message, double index) {
		super();
		this.id = message.getId();
		this.authorId = message.getAuthorId();
		this.clientId = message.getClientId();
		this.type = message.getType();
		this.time = (long)index;
		String content = message.getContent();
		if(Type.type_many_chat == type || Type.type_single_chat == type) {
			messageChat = JsonUtils.fromJson(content, MessageChat.class);
		} else {
			messageNotice = JsonUtils.fromJson(content, MessageNotice.class);
		}
		this.index = index;
	}

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

	public String getAuthorNick() {
		return authorNick;
	}

	public void setAuthorNick(String authorNick) {
		this.authorNick = authorNick;
	}

	public String getAuthorIcon() {
		return authorIcon;
	}

	public void setAuthorIcon(String authorIcon) {
		this.authorIcon = authorIcon;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public MessageChat getMessageChat() {
		return messageChat;
	}

	public void setMessageChat(MessageChat messageChat) {
		this.messageChat = messageChat;
	}

	public MessageNotice getMessageNotice() {
		return messageNotice;
	}

	public void setMessageNotice(MessageNotice messageNotice) {
		this.messageNotice = messageNotice;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public double getIndex() {
		return index;
	}

	public void setIndex(double index) {
		this.index = index;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

    public String getAuthorRemark() {
        return authorRemark;
    }

    public void setAuthorRemark(String authorRemark) {
        this.authorRemark = authorRemark;
    }
}
