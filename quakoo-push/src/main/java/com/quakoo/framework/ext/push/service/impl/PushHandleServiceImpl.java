package com.quakoo.framework.ext.push.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Resource;

import com.google.common.collect.Maps;
import com.quakoo.framework.ext.push.bean.PushMsg;
import com.quakoo.framework.ext.push.dao.PayloadDao;
import com.quakoo.framework.ext.push.dao.PushHandleQueueDao;
import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleQueue;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushHandleService;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class PushHandleServiceImpl extends BaseService implements PushHandleService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(PushHandleServiceImpl.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

    private final static int handle_num = 50;

    @Resource
    private PayloadDao payloadDao;

    @Resource
    private PushHandleQueueDao pushHandleQueueDao;

    private static volatile LinkedBlockingQueue<PushMsg> queue = new LinkedBlockingQueue<PushMsg>();

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class SubProcesser implements Runnable {

        private List<PushMsg> list;

        public SubProcesser(List<PushMsg> list) {
            this.list = list;
        }

        @Override
        public void run() {
            List<Payload> payloads = Lists.newArrayList();
            for (PushMsg one : list) {
                Payload payload = new Payload();
                payload.setId(one.getPayloadId());
                payload.setContent(one.getContent());
                payload.setTitle(one.getTitle());
                payload.setExtra(one.getExtra());
                payload.setPlatform(one.getPlatform());
                payloads.add(payload);
            }
            List<Payload> successPayloads = payloadDao.insert(payloads);
            Map<Long, Payload> successPayloadMap = Maps.newHashMap();
            for(Payload one : successPayloads) {
                successPayloadMap.put(one.getId(), one);
            }
            List<PushHandleQueue> pushHandleQueues = Lists.newArrayList();
            for (PushMsg one : list) {
                long payloadId = one.getPayloadId();
                Payload payload = successPayloadMap.get(payloadId);
                if (null != payload) {
                    PushHandleQueue pushHandleQueue = new PushHandleQueue();
                    pushHandleQueue.setId(payloadId);
                    pushHandleQueue.setType(one.getType());
                    pushHandleQueue.setUid(one.getUid());
                    pushHandleQueue.setUids(one.getUids());
                    pushHandleQueues.add(pushHandleQueue);
                }
            }
            if(pushHandleQueues.size() > 0) pushHandleQueueDao.insert(pushHandleQueues);
            list.clear();
            list = null;
        }
    }


    class Processer implements Runnable {
        @Override
        public void run() {
            List<PushMsg> batchList= Lists.newArrayList();
            while(true) {
                try {
                    PushMsg pushMsg = queue.take();
                    batchList.add(pushMsg);
                    if(batchList.size() >= handle_num) {
                        int payloadIdNum = batchList.size();
                        List<Long> payloadids = payloadDao.getPayloadIds(payloadIdNum);
                        for(int i = 0; i < batchList.size(); i++) {
                            PushMsg one = batchList.get(i);
                            long payloadid = payloadids.get(i);
                            one.setPayloadId(payloadid);
                        }
                        SubProcesser subProcesser = new SubProcesser(Lists.newArrayList(batchList));
                        executorService.submit(subProcesser);
                        batchList.clear();
                    } else {
                        logger.info("==== queue num : " + queue.size());
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void push(long uid, String title, String content,
                     Map<String, String> extra, int platform) throws Exception {
        PushMsg pushMsg = new PushMsg();
        pushMsg.setUid(uid);
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setType(PushHandleQueue.type_single);
        pushMsg.setPlatform(platform);
        queue.add(pushMsg);

//		Payload payload = new Payload();
//		payload.setContent(content);
//		payload.setTitle(title);
//		payload.setExtra(extra);
//        payload.setPlatform(platform);
//		payload = payloadDao.insert(payload);
//		long payloadId = payload.getId();
//		PushHandleQueue handleQueue = new PushHandleQueue();
//        handleQueue.setId(payloadId);
//		handleQueue.setType(PushHandleQueue.type_single);
//		handleQueue.setUid(uid);
//		pushHandleQueueDao.insert(handleQueue);
    }

    @Override
    public void batchPush(List<Long> uids, String title, String content,
                          Map<String, String> extra, int platform) throws Exception {
        if(uids.size() == 1) {
            long uid = uids.get(0);
//			this.push(uid, title, content, extra, platform);
            PushMsg pushMsg = new PushMsg();
            pushMsg.setUid(uid);
            pushMsg.setTitle(title);
            pushMsg.setContent(content);
            pushMsg.setExtra(extra);
            pushMsg.setType(PushHandleQueue.type_single);
            pushMsg.setPlatform(platform);
            queue.add(pushMsg);
        } else {
            PushMsg pushMsg = new PushMsg();
            pushMsg.setUids(StringUtils.join(uids, ","));
            pushMsg.setTitle(title);
            pushMsg.setContent(content);
            pushMsg.setExtra(extra);
            pushMsg.setType(PushHandleQueue.type_batch);
            pushMsg.setPlatform(platform);
            queue.add(pushMsg);

//			Payload payload = new Payload();
//			payload.setContent(content);
//			payload.setTitle(title);
//			payload.setExtra(extra);
//			payload.setPlatform(platform);
//			payload = payloadDao.insert(payload);
//			long payloadId = payload.getId();
//			PushHandleQueue handleQueue = new PushHandleQueue();
//			handleQueue.setId(payloadId);
////			handleQueue.setPayloadId(payloadId);
//			handleQueue.setType(PushHandleQueue.type_batch);
//			handleQueue.setUids(StringUtils.join(uids, ","));
//			pushHandleQueueDao.insert(handleQueue);
        }
    }

    public static void main(String[] args) {
        List<String> a = Lists.newArrayList("a", "b");
        List<String> b = Lists.newArrayList(a);
        a.clear();
        System.out.println(a.toString());
        System.out.println(b.toString());
    }

    @Override
    public List<PushHandleQueue> getHandleQueueItems(String tableName, int size)
            throws Exception {
        return pushHandleQueueDao.list(tableName, size);
    }

    @Override
    public void deleteQueueItem(PushHandleQueue one) throws Exception {
        pushHandleQueueDao.delete(one);
    }

    @Override
    public void deleteQueueItems(List<PushHandleQueue> list) throws Exception {
        pushHandleQueueDao.delete(list);
    }

    @Override
    public List<Payload> getPayloads(List<Long> pids) throws Exception {
        return payloadDao.load(pids);
    }
	
}
