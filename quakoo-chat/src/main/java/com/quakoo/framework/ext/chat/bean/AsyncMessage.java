package com.quakoo.framework.ext.chat.bean;

import com.quakoo.framework.ext.chat.model.param.nio.SessionResponse;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

public class AsyncMessage implements Serializable {

    private long id;

    private long authorId;

    private String clientId;

    private int type;

    private long thirdId;

    private String content;

    private long time;

    private ChannelHandlerContext ctx;

    private SessionResponse response;

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

    public long getThirdId() {
        return thirdId;
    }

    public void setThirdId(long thirdId) {
        this.thirdId = thirdId;
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

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public SessionResponse getResponse() {
        return response;
    }

    public void setResponse(SessionResponse response) {
        this.response = response;
    }
}
