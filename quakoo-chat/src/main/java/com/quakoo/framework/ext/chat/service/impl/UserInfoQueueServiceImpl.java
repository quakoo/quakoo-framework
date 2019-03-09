package com.quakoo.framework.ext.chat.service.impl;

import com.quakoo.framework.ext.chat.dao.UserInfoQueueDao;
import com.quakoo.framework.ext.chat.service.UserInfoQueueService;

import javax.annotation.Resource;
import java.util.List;

public class UserInfoQueueServiceImpl implements UserInfoQueueService {

    @Resource
    private UserInfoQueueDao userInfoQueueDao;

    @Override
    public List<Long> list(String queueName, int size) throws Exception {
        return userInfoQueueDao.list(queueName, size);
    }

    @Override
    public void delete(List<Long> uids) throws Exception {
        userInfoQueueDao.delete(uids);
    }
}
