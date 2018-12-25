package com.quakoo.framework.ext.push.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.dao.PayloadDao;
import com.quakoo.framework.ext.push.dao.PushHandleAllQueueDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueInfoDao;
import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleAllQueue;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushHandleAllService;

public class PushHandleAllServiceImpl extends BaseService implements PushHandleAllService {

	@Resource
	private PushUserQueueInfoDao pushUserQueueInfoDao;
	
	@Resource
	private PushHandleAllQueueDao pushHandleAllQueueDao;
	
	@Resource
	private PushUserQueueDao pushUserQueueDao;
	
	@Resource
	private PayloadDao payloadDao;

	@Override
	public void initPushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo)
			throws Exception {
		pushUserQueueInfoDao.insert(pushUserQueueInfo);
	}

	@Override
	public PushUserQueueInfo loadPushUserQueueInfo(String tableName)
			throws Exception {
		return pushUserQueueInfoDao.load(tableName);
	}

	@Override
	public boolean updatePushUserQueueInfo(PushUserQueueInfo pushUserQueueInfo)
			throws Exception {
		return pushUserQueueInfoDao.update(pushUserQueueInfo);
	}

	@Override
	public PushHandleAllQueue nextPushHandleAllQueueItem(long phaqid)
			throws Exception {
		List<PushHandleAllQueue> list = pushHandleAllQueueDao.getList(phaqid, 1);
		if(null == list || list.size() == 0) {
			return null;
		} else {
			return list.get(0);
		}
	}

	@Override
	public List<PushUserQueue> getPushUserQueueItems(String table_name,
                                                     long index, int size) throws Exception {
		return pushUserQueueDao.getList(table_name, index, size);
	}

	@Override
	public Payload loadPayload(long pid) throws Exception {
		return payloadDao.load(pid);
	}

	@Override
	public void push(String title, String content, Map<String, String> extra, int platform)
			throws Exception {
		Payload payload = new Payload();
		payload.setContent(content);
		payload.setTitle(title);
		payload.setExtra(extra);
		payload.setPlatform(platform);
		payload = payloadDao.insert(payload);
		long payloadId = payload.getId();
		PushHandleAllQueue handleAllQueue = new PushHandleAllQueue();
		handleAllQueue.setPayloadId(payloadId);
		pushHandleAllQueueDao.insert(handleAllQueue);
	}

	@Override
	public PushHandleAllQueue currentPushHandleAllQueueItem(long phaqid)
			throws Exception {
		return pushHandleAllQueueDao.load(phaqid);
	}
	
}
