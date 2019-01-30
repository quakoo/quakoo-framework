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

/**
 * 推送消息类
 * class_name: PushMsgServiceImpl
 * package: com.quakoo.framework.ext.push.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 14:49
 **/
public class PushMsgServiceImpl extends BaseService implements PushMsgService, InitializingBean {

    private Logger pushMsgLogger = LoggerFactory.getLogger("push_msg"); //特殊的日志记录推送的消息

    private final static int handle_num = 30; //一次处理的条数

    private final static int handle_expire_time = 1000 * 60 * 1; //超时处理的时间

    private int persistence; //是否持久化

    @Resource
    private PushMsgDao pushMsgDao;

    private static volatile LinkedBlockingQueue<PushMsg> queue = new LinkedBlockingQueue<PushMsg>(); //待处理的队列

    @Override
    public void afterPropertiesSet() throws Exception {
        persistence = Integer.parseInt(pushInfo.persistence);
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    /**
     * 处理线程
     * class_name: PushMsgServiceImpl
     * package: com.quakoo.framework.ext.push.service.impl
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:56
     **/
    class Processer implements Runnable {

        /**
         * 批量处理
         * method_name: handle
         * params: [list]
         * return: void
         * creat_user: lihao
         * creat_date: 2019/1/30
         * creat_time: 14:56
         **/
        private void handle(List<PushMsg> list) throws Exception {
            Set<Long> exists = Sets.newHashSet();
            List<PushMsg> insertList = Lists.newArrayList();
            List<PushMsg> updateList = Lists.newArrayList();
            for(int i = list.size() - 1; i >= 0; i--) {
                PushMsg one = list.get(i);
                if(!exists.contains(one.getId())) { //如果已经处理过，不再重复处理
                    if(one.getStatus() == PushMsg.status_wait) {
                        insertList.add(one);
                    } else {
                        updateList.add(one);
                    }
                    exists.add(one.getId());
                }
            }
            if(insertList.size() > 0) pushMsgDao.insert(insertList); //处理待发送的
            if(updateList.size() > 0) pushMsgDao.update(updateList); //处理已发送的
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

    /**
     * 讲待处理的消息放入队列
     * method_name: add
     * params: [pushMsg, status]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:57
     **/
    private void add(PushMsg pushMsg, int status) {
        pushMsg.setStatus(status);
        queue.add(pushMsg);
    }

    /**
     * 生成消息ID
     * method_name: createId
     * params: []
     * return: long
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:58
     **/
    @Override
    public long createId() {
        return pushMsgDao.createId();
    }

    /**
     * 接收待推送消息
     * method_name: accept
     * params: [pushMsg]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:58
     **/
    @Override
    public void accept(PushMsg pushMsg){
        if(persistence == 1) add(pushMsg, PushMsg.status_wait);
        pushMsgLogger.info("func:{},pushMsg:{},time:{}", new Object[]{"accept", JsonUtils.toJson(pushMsg), System.currentTimeMillis()});
    }

    /**
     * 完成推送消息
     * method_name: finish
     * params: [pushMsg]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:58
     **/
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
