package com.quakoo.framework.ext.push.service;



import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;

import java.util.List;
import java.util.Map;

public interface PushMsgHandleService {

    public void push(long uid, String title, String content,
                     Map<String, String> extra, int platform) throws Exception;

    public void batchPush(List<Long> uids, String title, String content,
                          Map<String, String> extra, int platform) throws Exception;

    public void allPush(String title, String content, Map<String, String> extra, int platform) throws Exception;




    public List<PushMsg> getHandlePushMsgs(String queueName, int size) throws Exception;

    public void finishHandlePushMsgs(String queueName, List<PushMsg> list) throws Exception;



    public void initPushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception;

    public PushUserQueueInfo loadPushUserQueueInfo(String tableName) throws Exception;

    public boolean updatePushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception;

    public PushMsgHandleAllQueue nextPushMsgHandleAllQueueItem(long phaqid) throws Exception;

    public PushMsgHandleAllQueue currentPushMsgHandleAllQueueItem(long phaqid) throws Exception;

    public List<PushUserQueue> getPushUserQueueItems(String table_name, long index, int size) throws Exception;


}
