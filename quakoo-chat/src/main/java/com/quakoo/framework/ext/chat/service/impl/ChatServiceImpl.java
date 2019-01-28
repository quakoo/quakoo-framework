package com.quakoo.framework.ext.chat.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.bean.AsyncMessage;
import com.quakoo.framework.ext.chat.dao.ManyChatQueueDao;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.dao.SingleChatQueueDao;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.MessageChat;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.model.ext.RecallExt;
import com.quakoo.framework.ext.chat.model.param.nio.ChatResponse;
import com.quakoo.framework.ext.chat.model.param.nio.OtherChatResponse;
import com.quakoo.framework.ext.chat.model.param.nio.RecallResponse;
import com.quakoo.framework.ext.chat.model.param.nio.SessionResponse;
import com.quakoo.framework.ext.chat.nio.ChannelUtils;
import com.quakoo.framework.ext.chat.service.ChatService;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ChatServiceImpl implements ChatService, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Resource
    private MessageDao messageDao;

//	@Resource
//	private UserClientInfoDao userClientInfoDao;

    @Resource
    private SingleChatQueueDao singleChatQueueDao;

    @Resource
    private ManyChatQueueDao manyChatQueueDao;

    private final static int handle_num = 10;

    private final static int handle_expire_time = 200;

    private static volatile LinkedBlockingQueue<AsyncMessage> queue = new LinkedBlockingQueue<AsyncMessage>();

    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class Processer implements Runnable {

        private void handle(final List<AsyncMessage> asyncMessages) throws Exception {
            List<Long> msgIds = messageDao.getMessageIds(asyncMessages.size());
            for(int i = 0; i < asyncMessages.size(); i++) {
                AsyncMessage asyncMessage = asyncMessages.get(i);
                asyncMessage.setId(msgIds.get(i));
            }
            List<Message> messages = Lists.newArrayList();
            for(AsyncMessage one : asyncMessages) {
                Message message = new Message();
                message.setId(one.getId());
                message.setAuthorId(one.getAuthorId());
                message.setType(one.getType());
                message.setThirdId(one.getThirdId());
                message.setClientId(one.getClientId());
                message.setContent(one.getContent());
                messages.add(message);
            }
            List<Message> successMessages = messageDao.insert(messages);
            final List<Long> successMids = Lists.newArrayList();
            for(Message one : successMessages) {
                successMids.add(one.getId());
            }
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    for(AsyncMessage asyncMessage : asyncMessages) {
                        if(!successMids.contains(asyncMessage.getId())) {
                            SessionResponse response = asyncMessage.getResponse();
                            response.setSuccess(false);
                            ChannelUtils.write(asyncMessage.getCtx(), response, false);
                        }
                    }
                }
            });
            List<SingleChatQueue> singleChatQueues = Lists.newArrayList();
            List<ManyChatQueue> manyChatQueues = Lists.newArrayList();
            for(Message successMessage : successMessages) {
                if(successMessage.getType() == Type.type_single_chat) {
                    SingleChatQueue singleChatQueue = new SingleChatQueue();
                    singleChatQueue.setUid(successMessage.getAuthorId());
                    singleChatQueue.setToUid(successMessage.getThirdId());
                    singleChatQueue.setMid(successMessage.getId());
                    singleChatQueue.setStatus(Status.unfinished);
                    singleChatQueue.setTime(System.currentTimeMillis());
                    singleChatQueues.add(singleChatQueue);
                } else if(successMessage.getType() == Type.type_many_chat) {
                    ManyChatQueue manyChatQueue = new ManyChatQueue();
                    manyChatQueue.setUid(successMessage.getAuthorId());
                    manyChatQueue.setCgid(successMessage.getThirdId());
                    manyChatQueue.setMid(successMessage.getId());
                    manyChatQueue.setStatus(Status.unfinished);
                    manyChatQueue.setTime(System.currentTimeMillis());
                    manyChatQueues.add(manyChatQueue);
                }
            }
            final List<Long> successIds = Lists.newArrayList();
            if(singleChatQueues.size() > 0) {
                List<SingleChatQueue> successQueues = singleChatQueueDao.insert(singleChatQueues);
                for(SingleChatQueue one : successQueues) {
                    successIds.add(one.getMid());
                }
            }
            if(manyChatQueues.size() > 0) {
                List<ManyChatQueue> successQueues = manyChatQueueDao.insert(manyChatQueues);
                for(ManyChatQueue one : successQueues) {
                    successIds.add(one.getMid());
                }
            }
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    for(AsyncMessage asyncMessage : asyncMessages) {
                        if(successIds.contains(asyncMessage.getId())) {
                            SessionResponse response = asyncMessage.getResponse();
                            response.setSuccess(true);
                            ChannelUtils.write(asyncMessage.getCtx(), response, false);
                        } else {
                            SessionResponse response = asyncMessage.getResponse();
                            response.setSuccess(false);
                            ChannelUtils.write(asyncMessage.getCtx(), response, false);
                        }
                    }
                }
            });
        }

        @Override
        public void run() {
            List<AsyncMessage> batchList = Lists.newArrayList();
            while(true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    AsyncMessage asyncMessage = queue.poll(200, TimeUnit.MILLISECONDS);
                    if (null != asyncMessage) {
                        batchList.add(asyncMessage);
                        if (batchList.size() >= handle_num) {
                            handle(Lists.newArrayList(batchList));
                        }
                    }
                    List<AsyncMessage> list = Lists.newArrayList();
                    for (Iterator<AsyncMessage> it = batchList.iterator(); it.hasNext(); ) {
                        AsyncMessage one = it.next();
                        if (currentTime - one.getTime() > handle_expire_time) {
                            list.add(one);
                            it.remove();
                        }
                    }
                    if (list.size() > 0) {
                        handle(list);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private boolean _insert(long uid, String clientId, int type, long thirdId, String content) {
        boolean res = false;
        Message message = new Message();
        message.setAuthorId(uid);
        message.setClientId(clientId);
        message.setType(type);
        message.setThirdId(thirdId);
        message.setContent(content);
        message = messageDao.insert(message);
        if(null != message) res = true;
//        UserClientInfo clientInfo = new UserClientInfo();
//        clientInfo.setUid(uid);
//        clientInfo.setType(type);
//        clientInfo.setThirdId(thirdId);
//        clientInfo.setMid(mid);
//        clientInfo.setClientId(clientId);
//        clientInfo.setCtime(System.currentTimeMillis());
//        res = userClientInfoDao.insert(clientInfo);
        if(res) {
            long mid = message.getId();
            if(Type.type_single_chat == type) {
                SingleChatQueue item = new SingleChatQueue();
                item.setUid(uid);
                item.setToUid(thirdId);
                item.setMid(mid);
                item.setStatus(Status.unfinished);
                item.setTime(System.currentTimeMillis());
                res = singleChatQueueDao.insert(item);
            } else if(Type.type_many_chat == type) {
//                ChatGroup chatGroup = chatGroupDao.load(thirdId);
//                if(null != chatGroup) {
//                    List<Long> uids = JsonUtils.fromJson(chatGroup.getUids(),
//                            new TypeReference<ArrayList<Long>>() {});
//                    if(uids.contains(uid)) {
                ManyChatQueue item = new ManyChatQueue();
                item.setUid(uid);
                item.setCgid(thirdId);
                item.setMid(mid);
                item.setStatus(Status.unfinished);
                item.setTime(System.currentTimeMillis());
                res = manyChatQueueDao.insert(item);
//                    } else {
//                        res = false;
//                    }
//                } else {
//                    res = false;
//                }
            }
        }
        return res;
    }

    @Override
    public void asyncRecall(long uid, String clientId, int type, long thirdId, long oldMid, ChannelHandlerContext ctx, String sessionId) throws Exception {
        RecallResponse response = new RecallResponse();
        response.setMid(oldMid);
        response.setSessionId(sessionId);
        if(Type.type_single_chat == type) {
            if(uid == thirdId) {
                response.setSuccess(false);
                ChannelUtils.write(ctx, response, false);
                return;
            }
        }
        Message oldMessage = messageDao.load(oldMid);
        if(null == oldMessage) {
            response.setSuccess(false);
            ChannelUtils.write(ctx, response, false);
            return;
        }
        String recallExt = JsonUtils.toJson(new RecallExt(oldMid));
        MessageChat messageChat = new MessageChat(recallExt);
        String content = JsonUtils.toJson(messageChat);
        AsyncMessage asyncMessage = new AsyncMessage();
        asyncMessage.setAuthorId(uid);
        asyncMessage.setClientId(clientId);
        asyncMessage.setType(type);
        asyncMessage.setThirdId(thirdId);
        asyncMessage.setContent(content);
        asyncMessage.setCtx(ctx);
        asyncMessage.setResponse(response);
        asyncMessage.setTime(System.currentTimeMillis());
        queue.add(asyncMessage);
    }

    @Override
    public boolean recall(long uid, String clientId, int type, long thirdId, long oldMid) throws Exception {
        if(Type.type_single_chat == type) {
            if(uid == thirdId) return false;
        }

        boolean res = false;
        Message oldMessage = messageDao.load(oldMid);
        if(null != oldMessage) {
            String recallExt = JsonUtils.toJson(new RecallExt(oldMid));
            MessageChat messageChat = new MessageChat(recallExt);
            String content = JsonUtils.toJson(messageChat);
            res = _insert(uid, clientId, type, thirdId, content);
        }
        return res;
    }

    @Override
    public void asyncOtherChat(long uid, String clientId, int type, long thirdId, MessageChat messageChat, ChannelHandlerContext ctx, String sessionId) throws Exception {
        OtherChatResponse response = new OtherChatResponse();
        response.setClientId(clientId);
        response.setSessionId(sessionId);
        if(Type.type_single_chat == type) {
            if(uid == thirdId) {
                response.setSuccess(false);
                ChannelUtils.write(ctx, response, false);
                return;
            }
        }
        String content = JsonUtils.toJson(messageChat);
        AsyncMessage asyncMessage = new AsyncMessage();
        asyncMessage.setAuthorId(uid);
        asyncMessage.setClientId(clientId);
        asyncMessage.setType(type);
        asyncMessage.setThirdId(thirdId);
        asyncMessage.setContent(content);
        asyncMessage.setCtx(ctx);
        asyncMessage.setResponse(response);
        asyncMessage.setTime(System.currentTimeMillis());
        queue.add(asyncMessage);
    }

    @Override
    public boolean otherChat(long uid, String clientId, int type, long thirdId, MessageChat messageChat) throws Exception {
        if(Type.type_single_chat == type) {
            if(uid == thirdId) return false;
        }
        boolean res = false;
        String content = JsonUtils.toJson(messageChat);
        res = _insert(uid, clientId, type, thirdId, content);
        return res;
    }

    @Override
    public void asyncChat(long uid, String clientId, int type, long thirdId, String word, String picture, String voice,
                          String voiceDuration, String video, String videoDuration, String ext, ChannelHandlerContext ctx, String sessionId) throws Exception {
        ChatResponse response = new ChatResponse();
        response.setClientId(clientId);
        response.setSessionId(sessionId);
        if(Type.type_single_chat == type) {
            if(uid == thirdId) {
                response.setSuccess(false);
                ChannelUtils.write(ctx, response, false);
                return;
            }
        }
        MessageChat messageChat = new MessageChat(word, picture, voice, voiceDuration, video, videoDuration, ext);
        String content = JsonUtils.toJson(messageChat);
        AsyncMessage asyncMessage = new AsyncMessage();
        asyncMessage.setAuthorId(uid);
        asyncMessage.setClientId(clientId);
        asyncMessage.setType(type);
        asyncMessage.setThirdId(thirdId);
        asyncMessage.setContent(content);
        asyncMessage.setCtx(ctx);
        asyncMessage.setResponse(response);
        asyncMessage.setTime(System.currentTimeMillis());
        queue.add(asyncMessage);
    }

    public boolean chat(long uid, String clientId, int type, long thirdId,
                        String word, String picture, String voice, String voiceDuration,
                        String video, String videoDuration, String ext) throws Exception {
        if(Type.type_single_chat == type) {
            if(uid == thirdId) return false;
        }

        boolean res = false;
        MessageChat messageChat = new MessageChat(word, picture, voice, voiceDuration, video, videoDuration, ext);
        String content = JsonUtils.toJson(messageChat);
        res = _insert(uid, clientId, type, thirdId, content);
        return res;
    }

    public boolean checkChat(long uid, String clientId) throws Exception {
        boolean res = false;
        Message message = messageDao.tempLoad(uid, clientId);
//		UserClientInfo clientInfo = new UserClientInfo();
//		clientInfo.setUid(uid);
//		clientInfo.setClientId(clientId);
//		clientInfo = userClientInfoDao.load(clientInfo);
        if(null != message) {
            int type = message.getType();
            long thirdId = message.getThirdId();
            long mid = message.getId();
            if(Type.type_single_chat == type) {
                SingleChatQueue item = new SingleChatQueue();
                item.setUid(uid);
                item.setToUid(thirdId);
                item.setMid(mid);
                res = singleChatQueueDao.exist(item);
            } else if(Type.type_many_chat == type) {
                ManyChatQueue item = new ManyChatQueue();
                item.setUid(uid);
                item.setCgid(thirdId);
                item.setMid(mid);
                res = manyChatQueueDao.exist(item);
            }
        }
        return res;
    }

    public static void main(String[] args) {
        Map<String, String> map = Maps.newHashMap();
        map.put("1", "1");
        String voice = JsonUtils.toJson(map);
        System.out.println(voice);
        Map<String, String> map2 = Maps.newHashMap();
        map2.put("voice", voice);
        String res = JsonUtils.toJson(map2);
        System.out.println(res);
    }

}
