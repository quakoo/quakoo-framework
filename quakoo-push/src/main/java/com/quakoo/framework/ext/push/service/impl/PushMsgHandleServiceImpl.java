package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.push.dao.PushMsgHandleAllQueueDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueInfoDao;
import com.quakoo.framework.ext.push.model.*;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushMsgHandleService;
import com.quakoo.framework.ext.push.service.PushMsgService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推送处理类
 * class_name: PushMsgHandleServiceImpl
 * package: com.quakoo.framework.ext.push.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 14:07
 **/
public class PushMsgHandleServiceImpl extends BaseService implements PushMsgHandleService, InitializingBean {

    private static String push_msg_queue_key; //推送通知队列名称

    @Resource
    private PushMsgService pushMsgService;

    @Resource
    private PushMsgHandleAllQueueDao pushMsgHandleAllQueueDao;

    @Resource
    private PushUserQueueInfoDao pushUserQueueInfoDao;

    @Resource
    private PushUserQueueDao pushUserQueueDao;

    /**
     * 获取队列名称(根据id获取队列名)
     * method_name: getQueueName
     * params: [id]
     * return: java.lang.String
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:12
     **/
    private String getQueueName(long id){
        long index = id % pushInfo.push_msg_queue_names.size();
        return pushInfo.push_msg_queue_names.get((int) index);
    }

    private JedisX cache;

    @Override
    public void afterPropertiesSet() throws Exception {
        push_msg_queue_key = pushInfo.projectName + "_%s";
        cache = new JedisX(pushInfo.redisInfo, pushInfo.redisConfig, 2000);
    }

    /**
     * 推送单个用户
     * method_name: push
     * params: [uid, title, content, extra, platform]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:13
     **/
    @Override
    public void push(long uid, String title, String content, Map<String, String> extra, int platform) throws Exception {
        long id = pushMsgService.createId(); //创建消息ID
        PushMsg pushMsg = new PushMsg();
        pushMsg.setId(id);
        pushMsg.setType(PushMsg.type_single);
        pushMsg.setUid(uid);
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setPlatform(platform);
        pushMsg.setTime(System.currentTimeMillis());

        pushMsgService.accept(pushMsg); //记录接受到消息

        String queueKey = String.format(push_msg_queue_key, getQueueName(id));
        cache.zaddObject(queueKey, new Double(pushMsg.getTime()), pushMsg); //放入到队列
    }

    /**
     * 推送多个用户
     * method_name: batchPush
     * params: [uids, title, content, extra, platform]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:32
     **/
    @Override
    public void batchPush(List<Long> uids, String title, String content, Map<String, String> extra, int platform) throws Exception {
        long id = pushMsgService.createId(); //创建消息ID
        PushMsg pushMsg = new PushMsg();
        pushMsg.setId(id);
        pushMsg.setType(PushMsg.type_batch);
        pushMsg.setUids(StringUtils.join(uids, ","));
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setPlatform(platform);
        pushMsg.setTime(System.currentTimeMillis());

        pushMsgService.accept(pushMsg); //记录接受到消息

        String queueKey = String.format(push_msg_queue_key, getQueueName(id));
        cache.zaddObject(queueKey, new Double(pushMsg.getTime()), pushMsg);  //放入到队列
    }

    /**
     * 推送所有用户
     * method_name: allPush
     * params: [title, content, extra, platform]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:34
     **/
    @Override
    public void allPush(String title, String content, Map<String, String> extra, int platform) throws Exception {
        long id = pushMsgService.createId(); //创建消息ID
        PushMsg pushMsg = new PushMsg();
        pushMsg.setId(id);
        pushMsg.setType(PushMsg.type_all);
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setPlatform(platform);
        pushMsg.setTime(System.currentTimeMillis());

        pushMsgService.accept(pushMsg); //记录接受到消息

        PushMsgHandleAllQueue pushMsgHandleAllQueue = new PushMsgHandleAllQueue();
        pushMsgHandleAllQueue.setPushMsgId(pushMsg.getId());
        pushMsgHandleAllQueue.setTitle(pushMsg.getTitle());
        pushMsgHandleAllQueue.setContent(pushMsg.getContent());
        pushMsgHandleAllQueue.setExtra(extra);
        pushMsgHandleAllQueue.setPlatform(pushMsg.getPlatform());
        pushMsgHandleAllQueue.setTime(pushMsg.getTime());
        pushMsgHandleAllQueueDao.insert(pushMsgHandleAllQueue); //放入到所有用户的消息队列
    }

    /**
     * 从队列里获取待发送消息
     * method_name: getHandlePushMsgs
     * params: [queueName, size]
     * return: java.util.List<com.quakoo.framework.ext.push.model.PushMsg>
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:36
     **/
    @Override
    public List<PushMsg> getHandlePushMsgs(String queueName, int size) throws Exception {
        List<PushMsg> res = Lists.newArrayList();
        String queueKey = String.format(push_msg_queue_key, queueName);
        Set<Object> set = cache.zrangeByScoreObject(queueKey, 0, Double.MAX_VALUE, 0, size, null);
        if(null != set && set.size() > 0){
            for(Object one : set){
                res.add((PushMsg) one);
            }
        }
        return res;
    }

    /**
     * 从队列里移除已发送的消息
     * method_name: finishHandlePushMsgs
     * params: [queueName, list]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:37
     **/
    @Override
    public void finishHandlePushMsgs(String queueName, List<PushMsg> list) throws Exception {
        List<Object> params = Lists.newArrayList();
        for(PushMsg one : list) {
            params.add(one);
        }
        String queueKey = String.format(push_msg_queue_key, queueName);
        cache.zremMultiObject(queueKey, params);

        pushMsgService.finish(list); //记录完成消息
    }

    /**
     * 初始化推送用户信息表
     * method_name: initPushUserQueueInfo
     * params: [pushUserQueueInfo]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:39
     **/
    @Override
    public void initPushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception {
        pushUserQueueInfoDao.insert(pushUserQueueInfo);
    }

    /**
     * 获取推送用户信息表
     * method_name: loadPushUserQueueInfo
     * params: [tableName]
     * return: com.quakoo.framework.ext.push.model.PushUserQueueInfo
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:39
     **/
    @Override
    public PushUserQueueInfo loadPushUserQueueInfo(String tableName) throws Exception {
        return pushUserQueueInfoDao.load(tableName);
    }

    /**
     * 更新推送用户信息表
     * method_name: updatePushUserQueueInfo
     * params: [pushUserQueueInfo]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:41
     **/
    @Override
    public boolean updatePushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception {
        boolean res = pushUserQueueInfoDao.update(pushUserQueueInfo);
        if(res) {
            if(pushUserQueueInfo.getEnd() == PushUserQueueInfo.end_yes) { //如果信息已经推送完成
                boolean allEnd = true;
                Set<Long> phaqids = Sets.newHashSet();
                for(String tableName : pushInfo.push_user_queue_table_names) { //获取所有的推送用户表
                    PushUserQueueInfo one = pushUserQueueInfoDao.load(tableName);
                    if(one.getEnd() == PushUserQueueInfo.end_no) {
                        allEnd = false;
                        break;
                    }
                    phaqids.add(one.getPhaqid());
                }
                if(allEnd && phaqids.size() == 1) { //如果都完成并且是同一条消息，记录消息完成
                    long phaqid = phaqids.iterator().next();
                    PushMsgHandleAllQueue pushMsgHandleAllQueue = pushMsgHandleAllQueueDao.load(phaqid);
                    PushMsg pushMsg = new PushMsg();
                    pushMsg.setId(pushMsgHandleAllQueue.getPushMsgId());
                    pushMsg.setType(PushMsg.type_all);
                    pushMsg.setTitle(pushMsgHandleAllQueue.getTitle());
                    pushMsg.setContent(pushMsgHandleAllQueue.getContent());
                    pushMsg.setExtra(pushMsgHandleAllQueue.getExtra());
                    pushMsg.setPlatform(pushMsgHandleAllQueue.getPlatform());
                    pushMsg.setTime(pushMsgHandleAllQueue.getTime());
                    pushMsgService.finish(pushMsg); //记录完成消息
                }
            }
        }
        return res;
    }

    /**
     * 获取下一条要推送给所有用户的消息
     * method_name: nextPushMsgHandleAllQueueItem
     * params: [phaqid]
     * return: com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:46
     **/
    @Override
    public PushMsgHandleAllQueue nextPushMsgHandleAllQueueItem(long phaqid) throws Exception {
        List<PushMsgHandleAllQueue> list = pushMsgHandleAllQueueDao.getList(phaqid, 1);
        if(null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /**
     * 获取要推送给所有用户的消息
     * method_name: currentPushMsgHandleAllQueueItem
     * params: [phaqid]
     * return: com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:47
     **/
    @Override
    public PushMsgHandleAllQueue currentPushMsgHandleAllQueueItem(long phaqid) throws Exception {
        return pushMsgHandleAllQueueDao.load(phaqid);
    }

    /**
     * 根据游标获取要推送的UID
     * method_name: getPushUserQueueItems
     * params: [table_name, index, size]
     * return: java.util.List<com.quakoo.framework.ext.push.model.PushUserQueue>
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 14:48
     **/
    @Override
    public List<PushUserQueue> getPushUserQueueItems(String table_name, long index, int size) throws Exception {
        return pushUserQueueDao.getList(table_name, index, size);
    }
}
