package com.quakoo.framework.ext.chat.service.impl.nio;

import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.ext.ChatCheckRes;
import com.quakoo.framework.ext.chat.model.ext.OtherChatRes;
import com.quakoo.framework.ext.chat.model.param.nio.*;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.ext.ChatCheckService;
import com.quakoo.framework.ext.chat.service.ext.OtherChatPrevService;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.quakoo.framework.ext.chat.context.handle.nio.NioLongConnectionContextHandle;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.nio.ChannelUtils;
import com.quakoo.framework.ext.chat.service.ChatService;
import com.quakoo.framework.ext.chat.service.UserInfoService;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import com.quakoo.framework.ext.chat.service.nio.NioConnectService;

/**
 * 长连接处理类
 * class_name: NioConnectServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl.nio
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:14
 **/
public class NioConnectServiceImpl implements NioConnectService, InitializingBean {
	
	Logger logger = LoggerFactory.getLogger(NioConnectServiceImpl.class);
	
	private LinkedBlockingQueue<NioConnectQueueItem> connect_queue = new LinkedBlockingQueue<NioConnectQueueItem>(); //长连接消息条目队列
	
	private final int threadNum = Runtime.getRuntime().availableProcessors() * 2 + 1;
	
	@Resource
	private UserInfoService userInfoService;
	
	@Resource
	private UserStreamService userStreamService;
	
	@Resource
	private ChatService chatService;

	@Resource
	private ChatCheckService chatCheckService;

	@Resource
	private OtherChatPrevService otherChatPrevService;

	@Override
	public void afterPropertiesSet() throws Exception {
		for (int i = 0; i < threadNum; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						NioConnectQueueItem queueItem = null;
						try {
							queueItem = connect_queue.take();
							handle(queueItem); //处理
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							if(null != queueItem) {
								ChannelHandlerContext ctx = queueItem.getCtx();
								ChannelUtils.write(ctx, new ErrorResponse("handle error"), false);
							}
						}
					}
				}
			}).start();
		}
	}

    public static void main(String[] args) {
        String ext = "{\"ext\":\"{\"type\":\"7\",\"extra\":\"\"}\"}";
    }

	/**
     * 接收到消息放入到队列
	 * method_name: handle
	 * params: [ctx, sessionRequest]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:15
	 **/
	@Override
	public void handle(ChannelHandlerContext ctx, SessionRequest sessionRequest) {
		NioConnectQueueItem queueItem = new NioConnectQueueItem(ctx, sessionRequest);
		connect_queue.add(queueItem);
	}
	
	private void handle(NioConnectQueueItem queueItem) {
		ChannelHandlerContext ctx = queueItem.getCtx();
		SessionRequest sessionRequest = queueItem.getSessionRequest();
		String sessionId = sessionRequest.getSessionId();
		if(sessionRequest instanceof PingRequest) {
			try {
				long uid = ((PingRequest) sessionRequest).getUid();
				double lastIndex = ((PingRequest) sessionRequest).getLastIndex();
				lastIndex += 0.001;
                UserInfo userInfo = userInfoService.load(uid);
                if(null != userInfo && userInfo.getLastIndex() > lastIndex) lastIndex = userInfo.getLastIndex();
                userInfo = userInfoService.syncUserInfo(uid, lastIndex, userInfo);
				userStreamService.init(uid);
				double lastPromptIndex = userInfo.getPromptIndex();
				
				NioUserLongConnection nioUserLongConnection = new NioUserLongConnection();
				nioUserLongConnection.setUid(uid);
				nioUserLongConnection.setLastMsgSort(lastIndex);
				nioUserLongConnection.setActiveTime(System.currentTimeMillis());
				NioLongConnectionContextHandle.connection_context.put(ctx, nioUserLongConnection);
				
				NioPromptQueueItem promptQueueItem = new NioPromptQueueItem();
				promptQueueItem.setCtx(ctx);
				promptQueueItem.setLastPromptIndex(lastPromptIndex);
			    promptQueueItem.setUid(uid);
			    NioLongConnectionContextHandle.prompt_queue.add(promptQueueItem);
			    
			    PongResponse response = new PongResponse();
			    response.setSessionId(sessionId);
			    response.setSuccess(true);
			    ChannelUtils.write(ctx, response, false);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				PongResponse response = new PongResponse();
				response.setSessionId(sessionId);
				response.setSuccess(false);
				response.setErrMsg(e.getMessage());
				ChannelUtils.write(ctx, response, false);
			}
		} else if(sessionRequest instanceof ChatRequest) {
			try {
				long uid = ((ChatRequest) sessionRequest).getUid();
				String clientId = ((ChatRequest) sessionRequest).getClientId();
				int chatType = ((ChatRequest) sessionRequest).getChatType();
				long thirdId = ((ChatRequest) sessionRequest).getThirdId();
				String word = ((ChatRequest) sessionRequest).getWord();
				String voice = ((ChatRequest) sessionRequest).getVoice();
				String voiceDuration = ((ChatRequest) sessionRequest).getVoiceDuration();
				String video = ((ChatRequest) sessionRequest).getVideo();
				String videoDuration = ((ChatRequest) sessionRequest).getVideoDuration();
				String picture = ((ChatRequest) sessionRequest).getPicture();
				String ext = ((ChatRequest) sessionRequest).getExt();
                if(StringUtils.isBlank(word) && StringUtils.isBlank(voice) && StringUtils.isBlank(picture)
                        && StringUtils.isBlank(video) && StringUtils.isBlank(ext)) {
                    ChatResponse response = new ChatResponse();
                    response.setClientId(clientId);
                    response.setSuccess(false);
                    response.setSessionId(sessionId);
                    response.setErrMsg("消息内容错误");
                    ChannelUtils.write(ctx, response, false);
                }
                ChatCheckRes chatCheckRes = chatCheckService.check(uid,chatType, thirdId, word);
                if(chatCheckRes.isSuccess()) {
                    chatService.asyncChat(uid, clientId, chatType, thirdId, word,
                            picture, voice, voiceDuration, video, videoDuration, ext, ctx, sessionId); //异步处理聊天消息
//                    boolean sign = chatService.chat(uid, clientId, chatType, thirdId, word,
//                            picture, voice, voiceDuration, video, videoDuration, ext);
//                    ChatResponse response = new ChatResponse();
//                    response.setClientId(clientId);
//                    response.setSuccess(sign);
//                    response.setSessionId(sessionId);
//                    ChannelUtils.write(ctx, response, false);
                } else {
                    ChatResponse response = new ChatResponse();
                    response.setClientId(clientId);
                    response.setSuccess(false);
                    response.setSessionId(sessionId);
                    response.setErrMsg(chatCheckRes.getMsg());
                    ChannelUtils.write(ctx, response, false);
                }
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				ChatResponse response = new ChatResponse();
				response.setSessionId(sessionId);
				response.setSuccess(false);
				response.setErrMsg(e.getMessage());
				ChannelUtils.write(ctx, response, false);
			}
		} else if(sessionRequest instanceof OtherChatRequest) {
            try {
                long uid = ((OtherChatRequest) sessionRequest).getUid();
                String clientId = ((OtherChatRequest) sessionRequest).getClientId();
                int chatType = ((OtherChatRequest) sessionRequest).getChatType();
                long thirdId = ((OtherChatRequest) sessionRequest).getThirdId();
                String word = ((OtherChatRequest) sessionRequest).getWord();
                String voice = ((OtherChatRequest) sessionRequest).getVoice();
                String voiceDuration = ((OtherChatRequest) sessionRequest).getVoiceDuration();
                String video = ((OtherChatRequest) sessionRequest).getVideo();
                String videoDuration = ((OtherChatRequest) sessionRequest).getVideoDuration();
                String picture = ((OtherChatRequest) sessionRequest).getPicture();
                String ext = ((OtherChatRequest) sessionRequest).getExt();

                ChatCheckRes chatCheckRes = chatCheckService.check(uid,chatType, thirdId, word);
                if(chatCheckRes.isSuccess()) {
                    OtherChatRes otherChatRes = otherChatPrevService.handle(uid, chatType, thirdId,
                            word, picture, voice, voiceDuration, video, videoDuration, ext);
                    if(otherChatRes.isSuccess()) {
                        chatService.asyncOtherChat(uid, clientId, chatType, thirdId,
                                otherChatRes.getMessageChat(), ctx, sessionId); //异步处理其他类型的消息
//                    boolean sign = chatService.otherChat(uid, clientId, chatType, thirdId,
//                            otherChatRes.getMessageChat());
//                    OtherChatResponse response = new OtherChatResponse();
//                    response.setClientId(clientId);
//                    response.setSuccess(sign);
//                    response.setSessionId(sessionId);
//                    ChannelUtils.write(ctx, response, false);
                    } else {
                        OtherChatResponse response = new OtherChatResponse();
                        response.setClientId(clientId);
                        response.setSuccess(false);
                        response.setSessionId(sessionId);
                        response.setErrMsg(otherChatRes.getErrMsg());
                        ChannelUtils.write(ctx, response, false);
                    }
                } else {
                    OtherChatResponse response = new OtherChatResponse();
                    response.setClientId(clientId);
                    response.setSuccess(false);
                    response.setSessionId(sessionId);
                    response.setErrMsg(chatCheckRes.getMsg());
                    ChannelUtils.write(ctx, response, false);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                OtherChatResponse response = new OtherChatResponse();
                response.setSessionId(sessionId);
                response.setSuccess(false);
                response.setErrMsg(e.getMessage());
                ChannelUtils.write(ctx, response, false);
            }
        } else if(sessionRequest instanceof RecallRequest) {
		    try {
                long uid = ((RecallRequest) sessionRequest).getUid();
                String clientId = ((RecallRequest) sessionRequest).getClientId();
                int chatType = ((RecallRequest) sessionRequest).getChatType();
                long thirdId = ((RecallRequest) sessionRequest).getThirdId();
                long oldMid = ((RecallRequest) sessionRequest).getMid();
                chatService.asyncRecall(uid, clientId, chatType, thirdId, oldMid, ctx, sessionId); //异步处理撤回消息
//                boolean sign = chatService.recall(uid, clientId, chatType, thirdId, oldMid);
//                RecallResponse response = new RecallResponse();
//                response.setMid(oldMid);
//                response.setSuccess(sign);
//                response.setSessionId(sessionId);
//                ChannelUtils.write(ctx, response, false);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                RecallResponse response = new RecallResponse();
                response.setSessionId(sessionId);
                response.setSuccess(false);
                response.setErrMsg(e.getMessage());
                ChannelUtils.write(ctx, response, false);
            }
        } else if(sessionRequest instanceof DeleteRequest) {
			try {
				long uid = ((DeleteRequest) sessionRequest).getUid();
				int chatType = ((DeleteRequest) sessionRequest).getChatType();
				long thirdId = ((DeleteRequest) sessionRequest).getThirdId();
				long mid = ((DeleteRequest) sessionRequest).getMid();
				boolean sign = userStreamService.delete(uid, chatType, thirdId, mid);
				DeleteResponse response = new DeleteResponse();
                response.setMid(mid);
				response.setSuccess(sign);
				response.setSessionId(sessionId);
				ChannelUtils.write(ctx, response, false);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				DeleteResponse response = new DeleteResponse();
				response.setSuccess(false);
				response.setSessionId(sessionId);
				response.setErrMsg(e.getMessage());
				ChannelUtils.write(ctx, response, false);
			}
		} else if(sessionRequest instanceof CheckRequest) {
			try {
				long uid =  ((CheckRequest) sessionRequest).getUid();
				String clientId = ((CheckRequest) sessionRequest).getClientId();
				boolean sign = chatService.checkChat(uid, clientId);
				CheckResponse response = new CheckResponse();
                response.setClientId(clientId);
				response.setSuccess(sign);
				response.setSessionId(sessionId);
				ChannelUtils.write(ctx, response, false);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				CheckResponse response = new CheckResponse();
				response.setSuccess(false);
				response.setSessionId(sessionId);
				response.setErrMsg(e.getMessage());
				ChannelUtils.write(ctx, response, false);
			}
		}
	}
	

}
