package com.quakoo.framework.ext.chat.service.impl;

import com.quakoo.framework.ext.chat.dao.UserStreamQueueDao;
import com.quakoo.framework.ext.chat.model.UserStreamQueue;
import com.quakoo.framework.ext.chat.service.UserStreamQueueService;

import javax.annotation.Resource;
import java.util.List;

public class UserStreamQueueServiceImpl implements UserStreamQueueService {

    @Resource
    private UserStreamQueueDao userStreamQueueDao;

    @Override
    public List<UserStreamQueue> list(String queueName, int size) throws Exception {
        return userStreamQueueDao.list(queueName, size);
    }

    @Override
    public void insert(List<UserStreamQueue> queues) throws Exception {
        userStreamQueueDao.insert(queues);
    }

    @Override
    public void delete(List<UserStreamQueue> queues) throws Exception {
        userStreamQueueDao.delete(queues);
    }

}
