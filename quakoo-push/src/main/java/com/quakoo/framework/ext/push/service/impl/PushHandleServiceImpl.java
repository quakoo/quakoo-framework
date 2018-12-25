package com.quakoo.framework.ext.push.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.dao.PayloadDao;
import com.quakoo.framework.ext.push.dao.PushHandleQueueDao;
import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleQueue;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushHandleService;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.transform.ListTransformUtils;
import com.quakoo.baseFramework.transform.ListTransformerStringToLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushHandleServiceImpl extends BaseService implements PushHandleService {

    Logger logger = LoggerFactory.getLogger(PushHandleServiceImpl.class);

	@Resource
	private PayloadDao payloadDao;
	
	@Resource
	private PushHandleQueueDao pushHandleQueueDao;

	@Override
	public void push(long uid, String title, String content,
			Map<String, String> extra, int platform) throws Exception {
		Payload payload = new Payload();
		payload.setContent(content);
		payload.setTitle(title);
		payload.setExtra(extra);
        payload.setPlatform(platform);
		payload = payloadDao.insert(payload);
		long payloadId = payload.getId();
		PushHandleQueue handleQueue = new PushHandleQueue();
		handleQueue.setPayloadId(payloadId);
		handleQueue.setType(PushHandleQueue.type_single);
		handleQueue.setUid(uid);
		pushHandleQueueDao.insert(handleQueue);
	}

	@Override
	public void batchPush(List<Long> uids, String title, String content,
			Map<String, String> extra, int platform) throws Exception {
		if(uids.size() == 1) {
			long uid = uids.get(0);
			this.push(uid, title, content, extra, platform);
		} else {
			Payload payload = new Payload();
			payload.setContent(content);
			payload.setTitle(title);
			payload.setExtra(extra);
			payload.setPlatform(platform);
			payload = payloadDao.insert(payload);
			long payloadId = payload.getId();
			PushHandleQueue handleQueue = new PushHandleQueue();
			handleQueue.setPayloadId(payloadId);
			handleQueue.setType(PushHandleQueue.type_batch);
			handleQueue.setUids(StringUtils.join(uids, ","));
			pushHandleQueueDao.insert(handleQueue);
		}
	}
	
	public static void main(String[] args) {
		List<Long> list = Lists.newArrayList(1l,2l);
		List<String> strList = Lists.newArrayList(
				StringUtils.split(StringUtils.join(list, ","), ","));
		list = ListTransformUtils.transformedList(strList, new ListTransformerStringToLong());
		System.out.println(list.toString());
	}

	@Override
	public List<PushHandleQueue> getHandleQueueItems(String tableName, int size)
			throws Exception {
		return pushHandleQueueDao.list(tableName, size);
	}

	@Override
	public void deleteQueueItem(PushHandleQueue one) throws Exception {
		pushHandleQueueDao.delete(one);
	}

	@Override
	public List<Payload> getPayloads(List<Long> pids) throws Exception {
		return payloadDao.load(pids);
	}
	
}
