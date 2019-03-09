package com.quakoo.framework.ext.chat.service;

import java.util.List;

public interface UserInfoQueueService {

    public List<Long> list(String queueName, int size) throws Exception;

    public void delete(List<Long> uids) throws Exception;

}
