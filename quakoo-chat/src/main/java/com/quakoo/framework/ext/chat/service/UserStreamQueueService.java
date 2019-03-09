package com.quakoo.framework.ext.chat.service;

import com.quakoo.framework.ext.chat.model.UserStreamQueue;

import java.util.List;

public interface UserStreamQueueService {

      public List<UserStreamQueue> list(String queueName, int size) throws Exception;

      public void insert(List<UserStreamQueue> queues) throws Exception;

      public void delete(List<UserStreamQueue> queues) throws Exception;

}
