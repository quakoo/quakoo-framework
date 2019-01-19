package com.quakoo.framework.ext.push.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.framework.ext.push.dao.PushMsgHandleAllQueueDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueInfoDao;
import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushMsgHandleService;
import com.quakoo.framework.ext.push.service.PushMsgService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PushMsgHandleServiceImpl extends BaseService implements PushMsgHandleService, InitializingBean {

    private static String push_msg_queue_key;

    @Resource
    private PushMsgService pushMsgService;

    @Resource
    private PushMsgHandleAllQueueDao pushMsgHandleAllQueueDao;

    @Resource
    private PushUserQueueInfoDao pushUserQueueInfoDao;

    @Resource
    private PushUserQueueDao pushUserQueueDao;

    private String getQueueName(long id){
        int index = (int) id % pushInfo.push_msg_queue_names.size();
        return pushInfo.push_msg_queue_names.get(index);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        push_msg_queue_key = pushInfo.projectName + "_%s";
    }

    @Override
    public void push(long uid, String title, String content, Map<String, String> extra, int platform) throws Exception {
        long id = pushMsgService.createId();
        PushMsg pushMsg = new PushMsg();
        pushMsg.setId(id);
        pushMsg.setType(PushMsg.type_single);
        pushMsg.setUid(uid);
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setPlatform(platform);
        pushMsg.setTime(System.currentTimeMillis());

        pushMsgService.accept(pushMsg);

        String queueKey = String.format(push_msg_queue_key, getQueueName(id));
        cache.zaddObject(queueKey, new Double(pushMsg.getTime()), pushMsg);
    }

    @Override
    public void batchPush(List<Long> uids, String title, String content, Map<String, String> extra, int platform) throws Exception {
        long id = pushMsgService.createId();
        PushMsg pushMsg = new PushMsg();
        pushMsg.setId(id);
        pushMsg.setType(PushMsg.type_batch);
        pushMsg.setUids(StringUtils.join(uids, ","));
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setPlatform(platform);
        pushMsg.setTime(System.currentTimeMillis());

        pushMsgService.accept(pushMsg);

        String queueKey = String.format(push_msg_queue_key, getQueueName(id));
        cache.zaddObject(queueKey, new Double(pushMsg.getTime()), pushMsg);
    }

    @Override
    public void allPush(String title, String content, Map<String, String> extra, int platform) throws Exception {
        long id = pushMsgService.createId();
        PushMsg pushMsg = new PushMsg();
        pushMsg.setId(id);
        pushMsg.setType(PushMsg.type_all);
        pushMsg.setTitle(title);
        pushMsg.setContent(content);
        pushMsg.setExtra(extra);
        pushMsg.setPlatform(platform);
        pushMsg.setTime(System.currentTimeMillis());

        pushMsgService.accept(pushMsg);

        PushMsgHandleAllQueue pushMsgHandleAllQueue = new PushMsgHandleAllQueue();
        pushMsgHandleAllQueue.setPushMsgId(pushMsg.getId());
        pushMsgHandleAllQueue.setTitle(pushMsg.getTitle());
        pushMsgHandleAllQueue.setContent(pushMsg.getContent());
        pushMsgHandleAllQueue.setExtra(extra);
        pushMsgHandleAllQueue.setPlatform(pushMsg.getPlatform());
        pushMsgHandleAllQueue.setTime(pushMsg.getTime());
        pushMsgHandleAllQueueDao.insert(pushMsgHandleAllQueue);
    }

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

    @Override
    public void finishHandlePushMsgs(String queueName, List<PushMsg> list) throws Exception {
        List<Object> params = Lists.newArrayList();
        for(PushMsg one : list) {
            params.add(one);
        }
        String queueKey = String.format(push_msg_queue_key, queueName);
        cache.zremMultiObject(queueKey, params);

        pushMsgService.finish(list);
    }

    @Override
    public void initPushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception {
        pushUserQueueInfoDao.insert(pushUserQueueInfo);
    }

    @Override
    public PushUserQueueInfo loadPushUserQueueInfo(String tableName) throws Exception {
        return pushUserQueueInfoDao.load(tableName);
    }

    @Override
    public boolean updatePushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception {
        boolean res = pushUserQueueInfoDao.update(pushUserQueueInfo);
        if(res) {
            if(pushUserQueueInfo.getEnd() == PushUserQueueInfo.end_yes) {
                boolean allEnd = true;
                Set<Long> phaqids = Sets.newHashSet();
                for(String tableName : pushInfo.push_user_queue_table_names) {
                    PushUserQueueInfo one = pushUserQueueInfoDao.load(tableName);
                    if(one.getEnd() == PushUserQueueInfo.end_no) {
                        allEnd = false;
                        break;
                    }
                    phaqids.add(one.getPhaqid());
                }
                if(allEnd && phaqids.size() == 1) {
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
                    pushMsgService.finish(pushMsg);
                }
            }
        }
        return res;
    }

    @Override
    public PushMsgHandleAllQueue nextPushMsgHandleAllQueueItem(long phaqid) throws Exception {
        List<PushMsgHandleAllQueue> list = pushMsgHandleAllQueueDao.getList(phaqid, 1);
        if(null == list || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    @Override
    public PushMsgHandleAllQueue currentPushMsgHandleAllQueueItem(long phaqid) throws Exception {
        return pushMsgHandleAllQueueDao.load(phaqid);
    }

    @Override
    public List<PushUserQueue> getPushUserQueueItems(String table_name, long index, int size) throws Exception {
        return pushUserQueueDao.getList(table_name, index, size);
    }
}
