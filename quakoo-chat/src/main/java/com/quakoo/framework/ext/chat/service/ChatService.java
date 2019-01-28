package com.quakoo.framework.ext.chat.service;

import com.quakoo.framework.ext.chat.model.MessageChat;
import io.netty.channel.ChannelHandlerContext;

public interface ChatService {

    public boolean chat(long uid, String clientId, int type, long thirdId,
                        String word, String picture, String voice, String voiceDuration,
                        String video, String videoDuration, String ext) throws Exception;

    public boolean otherChat(long uid, String clientId, int type, long thirdId,
                             MessageChat messageChat) throws Exception;

    public boolean checkChat(long uid, String clientId) throws Exception;

    public boolean recall(long uid, String clientId, int type, long thirdId, long mid) throws Exception;



    public void asyncChat(long uid, String clientId, int type, long thirdId,
                          String word, String picture, String voice, String voiceDuration,
                          String video, String videoDuration, String ext, ChannelHandlerContext ctx, String sessionId) throws Exception;

    public void asyncOtherChat(long uid, String clientId, int type, long thirdId,
                               MessageChat messageChat, ChannelHandlerContext ctx, String sessionId) throws Exception;

    public void asyncRecall(long uid, String clientId, int type, long thirdId, long mid, ChannelHandlerContext ctx, String sessionId) throws Exception;
	
}
