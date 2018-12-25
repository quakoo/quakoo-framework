package com.quakoo.framework.ext.push.context.handle;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleAllQueue;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;
import com.quakoo.framework.ext.push.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.push.distributed.DistributedConfig;
import com.quakoo.framework.ext.push.service.PushHandleAllService;

public class PushHandleAllSchedulerContextHandle extends PushBasePushHandleContextHandle {

	Logger logger = LoggerFactory
			.getLogger(PushHandleAllSchedulerContextHandle.class);

	private int baseHandleNum = 200;

	@Resource
	private PushHandleAllService pushHandleAllService;
	
	private CompletionService<Void> completionService = 
			new ExecutorCompletionService<Void>(executorService);

	@Override
	public void afterPropertiesSet() throws Exception {
		for(String tableName : pushInfo.push_user_queue_table_names) {
			Thread thread = new Thread(new Processer(tableName));
			thread.start();
		}
	}
	
	public static void main(String[] args) {
		List<Integer> list = Lists.newArrayList();
		for(int i = 1; i <= 101; i++) {
			list.add(i);
		}
		List<List<Integer>> res = Lists.partition(list, 200);
		for(List<Integer> one : res) {
			System.out.println(one.toString());
		}
	}

	class Processer implements Runnable {

		private String tableName;

		public Processer(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public void run() {
			while(true) {
				if(DistributedConfig.serverNum != 0 &&
						DistributedConfig.canRunUserQueueTable.contains(tableName)) {
					SleepUtils.sleep(200, 50);
					try {
						PushUserQueueInfo pushUserQueueInfo = pushHandleAllService.loadPushUserQueueInfo(tableName);
						if(null == pushUserQueueInfo) {
							pushUserQueueInfo = new PushUserQueueInfo();
							pushUserQueueInfo.setTableName(tableName);
							pushHandleAllService.initPushUserQueueInfo(pushUserQueueInfo);
						}
						long phaqid = pushUserQueueInfo.getPhaqid();
						int end = pushUserQueueInfo.getEnd();
						long index = pushUserQueueInfo.getIndex();
						if(phaqid == 0 || end == PushUserQueueInfo.end_yes) {
							PushHandleAllQueue pushHandleAllQueueItem = pushHandleAllService.
									nextPushHandleAllQueueItem(phaqid);
							if(null != pushHandleAllQueueItem) {
								long nextPhaqid = pushHandleAllQueueItem.getId();
								pushUserQueueInfo.setPhaqid(nextPhaqid);
								pushUserQueueInfo.setIndex(0);
								pushUserQueueInfo.setEnd(PushUserQueueInfo.end_no);
								pushHandleAllService.updatePushUserQueueInfo(pushUserQueueInfo);
							}
						} else {
							int handleNum = DistributedConfig.serverNum * baseHandleNum;
							List<PushUserQueue> pushUserQueueItems = pushHandleAllService.
									getPushUserQueueItems(tableName, index, handleNum);
							int currentSize = pushUserQueueItems.size();
							if(currentSize > 0) {
								PushHandleAllQueue pushHandleAllQueueItem = pushHandleAllService.
										currentPushHandleAllQueueItem(phaqid);
								long payloadId = pushHandleAllQueueItem.getPayloadId();
								final Payload payload = pushHandleAllService.loadPayload(payloadId);
								List<Long> uids = Lists.newArrayList();
								for(PushUserQueue pushUserQueueItem : pushUserQueueItems) {
									uids.add(pushUserQueueItem.getUid());
								}
								if(currentSize <= baseHandleNum) {
									handleBatch(uids, payload);
								} else {
									List<List<Long>> uidsList = Lists.partition(uids, baseHandleNum);
									for(int i = 0; i < uidsList.size(); i++) {
										final List<Long> subUids = uidsList.get(i);
										completionService.submit(new Callable<Void>() {
											@Override
											public Void call() throws Exception {
												handleBatch(subUids, payload);
												return null;
											}
										});
									}
									for (int i = 0; i < uidsList.size(); i++) {
									    completionService.take().get();
									}
								}
							}
							if(currentSize == handleNum) {
								long maxIndex = pushUserQueueItems.get(currentSize - 1).getIndex();
								pushUserQueueInfo.setIndex(maxIndex);
							} else {
								pushUserQueueInfo.setEnd(PushUserQueueInfo.end_yes);
							}
							pushHandleAllService.updatePushUserQueueInfo(pushUserQueueInfo);
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				} else {
					Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
				}
			}
		}
	}

}
