package com.quakoo.framework.ext.push.service;

import java.util.List;
import java.util.Map;

import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleQueue;

public interface PushHandleService {

    public void push(long uid, String title, String content,
                     Map<String, String> extra, int platform) throws Exception;

    public void batchPush(List<Long> uids, String title, String content,
                          Map<String, String> extra, int platform) throws Exception;

    public List<PushHandleQueue> getHandleQueueItems(String tableName, int size) throws Exception;

    public void deleteQueueItem(PushHandleQueue one) throws Exception;

    public void deleteQueueItems(List<PushHandleQueue> list) throws Exception;

    public List<Payload> getPayloads(List<Long> pids) throws Exception;
	
}
