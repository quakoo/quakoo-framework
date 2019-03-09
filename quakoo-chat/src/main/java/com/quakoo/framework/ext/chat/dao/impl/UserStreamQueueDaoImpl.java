package com.quakoo.framework.ext.chat.dao.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.UserStreamQueueDao;
import com.quakoo.framework.ext.chat.model.UserStreamQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserStreamQueueDaoImpl extends BaseDaoHandle implements UserStreamQueueDao, InitializingBean {

    private JedisX queueClient;

    private final static String user_stream_queue_key = "%s_user_stream_queue_%s";

    private Logger logger = LoggerFactory.getLogger(UserStreamQueueDaoImpl.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        queueClient = new JedisX(chatInfo.queueInfo, chatInfo.queueConfig, 5000);
    }

    private String getQueue(long uid) {
        long index = uid % chatInfo.user_stream_queue_names.size();
        return chatInfo.user_stream_queue_names.get((int) index);
    }


    @Override
    public void insert(List<UserStreamQueue> queues) throws DataAccessException {
        Map<String, List<UserStreamQueue>> queuesMap = Maps.newHashMap();
        for (UserStreamQueue queue : queues) {
            String queueName = getQueue(queue.getUid());
            List<UserStreamQueue> list = queuesMap.get(queueName);
            if (null == list) {
                list = Lists.newArrayList();
                queuesMap.put(queueName, list);
            }
            list.add(queue);
        }
        for (Map.Entry<String, List<UserStreamQueue>> entry : queuesMap.entrySet()) {
            String queueName = entry.getKey();
            List<UserStreamQueue> list = entry.getValue();
            String queue_key = String.format(user_stream_queue_key, chatInfo.projectName, queueName);
            Map<Object, Double> redisMap = Maps.newHashMap();
            for (UserStreamQueue one : list) {
                redisMap.put(one, new Double(one.getSort()));
            }
            queueClient.zaddMultiObject(queue_key, redisMap);
        }
    }

    @Override
    public void delete(List<UserStreamQueue> queues) throws DataAccessException {
        Map<String, List<Object>> queuesMap = Maps.newHashMap();
        for (UserStreamQueue queue : queues) {
            String queueName = getQueue(queue.getUid());
            List<Object> list = queuesMap.get(queueName);
            if (null == list) {
                list = Lists.newArrayList();
                queuesMap.put(queueName, list);
            }
            list.add(queue);
        }
        for (Map.Entry<String, List<Object>> entry : queuesMap.entrySet()) {
            String queueName = entry.getKey();
            List<Object> list = entry.getValue();
            String queue_key = String.format(user_stream_queue_key, chatInfo.projectName, queueName);
            queueClient.zremMultiObject(queue_key, list);
        }
    }

    @Override
    public List<UserStreamQueue> list(String queue_name, int size) throws DataAccessException {
        String queue_key = String.format(user_stream_queue_key, chatInfo.projectName, queue_name);
        List<UserStreamQueue> res = Lists.newArrayList();
        Set<Object> set = queueClient.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
        if (null != set && set.size() > 0) {
            for (Object one : set) {
                res.add((UserStreamQueue) one);
            }
        }
        return res;
    }

}
