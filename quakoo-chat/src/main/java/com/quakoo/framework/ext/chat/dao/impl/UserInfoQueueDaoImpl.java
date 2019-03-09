package com.quakoo.framework.ext.chat.dao.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.UserInfoQueueDao;
import com.quakoo.framework.ext.chat.model.UserInfoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserInfoQueueDaoImpl extends BaseDaoHandle implements UserInfoQueueDao, InitializingBean {

    private JedisX queueClient;

    private final static String user_info_queue_key = "%s_uid_info_queue_%s";

    private Logger logger = LoggerFactory.getLogger(UserInfoQueueDaoImpl.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        queueClient = new JedisX(chatInfo.queueInfo, chatInfo.queueConfig, 5000);
    }

    private String getQueue(long uid) {
        long index = uid % chatInfo.user_info_queue_names.size();
        return chatInfo.user_info_queue_names.get((int) index);
    }

    @Override
    public void insert(List<UserInfoQueue> queues) throws DataAccessException {
        Map<String, List<UserInfoQueue>> queuesMap = Maps.newHashMap();
        for (UserInfoQueue queue : queues) {
            String queueName = getQueue(queue.getUid());
            List<UserInfoQueue> list = queuesMap.get(queueName);
            if (null == list) {
                list = Lists.newArrayList();
                queuesMap.put(queueName, list);
            }
            list.add(queue);
        }
        for (Map.Entry<String, List<UserInfoQueue>> entry : queuesMap.entrySet()) {
            String queueName = entry.getKey();
            List<UserInfoQueue> list = entry.getValue();
            String queue_key = String.format(user_info_queue_key, chatInfo.projectName, queueName);
            Map<Object, Double> redisMap = Maps.newHashMap();
            for (UserInfoQueue one : list) {
                redisMap.put(one.getUid(), one.getLoginTime());
            }
            queueClient.zaddMultiObject(queue_key, redisMap);
        }
    }

    @Override
    public void delete(List<Long> uids) throws DataAccessException {
        Map<String, List<Object>> queuesMap = Maps.newHashMap();
        for (long uid : uids) {
            String queueName = getQueue(uid);
            List<Object> list = queuesMap.get(queueName);
            if (null == list) {
                list = Lists.newArrayList();
                queuesMap.put(queueName, list);
            }
            list.add(uid);
        }
        for (Map.Entry<String, List<Object>> entry : queuesMap.entrySet()) {
            String queueName = entry.getKey();
            List<Object> list = entry.getValue();
            String queue_key = String.format(user_info_queue_key, chatInfo.projectName, queueName);
            queueClient.zremMultiObject(queue_key, list);
        }
    }

    @Override
    public List<Long> list(String queue_name, int size) throws DataAccessException {
        String queue_key = String.format(user_info_queue_key, chatInfo.projectName, queue_name);
        List<Long> res = Lists.newArrayList();
        Set<Object> set = queueClient.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
        if (null != set && set.size() > 0) {
            for (Object one : set) {
                res.add((Long) one);
            }
        }
        return res;
    }

}
