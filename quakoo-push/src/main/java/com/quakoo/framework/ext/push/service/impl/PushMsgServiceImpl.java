package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.dao.PushMsgDao;
import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushMsgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PushMsgServiceImpl extends BaseService implements PushMsgService, InitializingBean {

    private Logger pushMsgLogger = LoggerFactory.getLogger("push_msg");

    private final static int handle_num = 30;

    private final static int handle_expire_time = 1000 * 60 * 1;

    private int persistence;

    @Resource
    private PushMsgDao pushMsgDao;

    private static volatile LinkedBlockingQueue<PushMsg> queue = new LinkedBlockingQueue<PushMsg>();

    @Override
    public void afterPropertiesSet() throws Exception {
        persistence = Integer.parseInt(pushInfo.persistence);
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class Processer implements Runnable {

        private void handle(List<PushMsg> list) throws Exception {
            Set<Long> exists = Sets.newHashSet();
            List<PushMsg> insertList = Lists.newArrayList();
            List<PushMsg> updateList = Lists.newArrayList();
            for(int i = list.size() - 1; i >= 0; i--) {
                PushMsg one = list.get(i);
                if(!exists.contains(one.getId())) {
                    if(one.getStatus() == PushMsg.status_wait) {
                        insertList.add(one);
                    } else {
                        updateList.add(one);
                    }
                    exists.add(one.getId());
                }
            }
            if(insertList.size() > 0) pushMsgDao.insert(insertList);
            if(updateList.size() > 0) pushMsgDao.update(updateList);
        }

        @Override
        public void run() {
            List<PushMsg> batchList = Lists.newArrayList();
            while (true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    PushMsg msg = queue.poll(1, TimeUnit.SECONDS);
                    if (null != msg) {
                        batchList.add(msg);
                        if (batchList.size() >= handle_num) {
                            handle(batchList);
                            batchList.clear();
                        }
                    }
                    List<PushMsg> list = Lists.newArrayList();
                    for (Iterator<PushMsg> it = batchList.iterator(); it.hasNext(); ) {
                        PushMsg one = it.next();
                        if (currentTime - one.getTime() > handle_expire_time) {
                            list.add(one);
                            it.remove();
                        }
                    }
                    if (list.size() > 0) {
                        handle(list);
                    }
                } catch (Exception e) {
                }
            }
        }

    }

    private void add(PushMsg pushMsg, int status) {
        pushMsg.setStatus(status);
        queue.add(pushMsg);
    }

    @Override
    public long createId() {
        return pushMsgDao.createId();
    }

    @Override
    public void accept(PushMsg pushMsg){
        if(persistence == 1) add(pushMsg, PushMsg.status_wait);
        pushMsgLogger.info("func:{},pushMsg:{},time:{}", new Object[]{"accept", JsonUtils.toJson(pushMsg), System.currentTimeMillis()});
    }

    @Override
    public void finish(PushMsg pushMsg){
        if(persistence == 1) add(pushMsg, PushMsg.status_send);
        pushMsgLogger.info("func:{},pushMsg:{},time:{}", new Object[]{"finish", JsonUtils.toJson(pushMsg), System.currentTimeMillis()});
    }

    @Override
    public void finish(List<PushMsg> pushMsgs) {
        for(PushMsg pushMsg : pushMsgs) {
            finish(pushMsg);
        }
    }

}
