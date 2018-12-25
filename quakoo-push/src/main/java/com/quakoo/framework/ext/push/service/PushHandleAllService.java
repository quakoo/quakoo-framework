package com.quakoo.framework.ext.push.service;



import java.util.List;
import java.util.Map;

import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleAllQueue;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;

public interface PushHandleAllService {

	public void initPushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception;

	public PushUserQueueInfo loadPushUserQueueInfo(String tableName) throws Exception;
	
	public boolean updatePushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo) throws Exception;
	
	public PushHandleAllQueue nextPushHandleAllQueueItem(long phaqid) throws Exception;
	
	public PushHandleAllQueue currentPushHandleAllQueueItem(long phaqid) throws Exception;
	
	public List<PushUserQueue> getPushUserQueueItems(String table_name, long index, int size) throws Exception;
	
	public Payload loadPayload(long pid) throws Exception;
	
	public void push(String title, String content, Map<String, String> extra, int platform) throws Exception;
	
}
