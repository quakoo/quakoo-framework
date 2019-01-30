package com.quakoo.framework.ext.push.context.handle;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue;
import com.quakoo.framework.ext.push.service.PushMsgHandleService;
import com.quakoo.framework.ext.push.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.push.distributed.DistributedConfig;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;

/**
 * 推送所有用户上下文
 * class_name: PushHandleAllSchedulerContextHandle
 * package: com.quakoo.framework.ext.push.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:09
 **/
public class PushHandleAllSchedulerContextHandle extends PushBasePushHandleContextHandle {

	Logger logger = LoggerFactory
			.getLogger(PushHandleAllSchedulerContextHandle.class);

	private int baseHandleNum = 200;

	@Resource
	private PushMsgHandleService pushMsgHandleService;
	
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

	/**
     * 处理线程
	 * class_name: PushHandleAllSchedulerContextHandle
	 * package: com.quakoo.framework.ext.push.context.handle
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:12
	 **/
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
						PushUserQueueInfo pushUserQueueInfo = pushMsgHandleService.loadPushUserQueueInfo(tableName); //读取推送信息表，查看推送到哪条通知
						if(null == pushUserQueueInfo) {
							pushUserQueueInfo = new PushUserQueueInfo();
							pushUserQueueInfo.setTableName(tableName);
                            pushMsgHandleService.initPushUserQueueInfo(pushUserQueueInfo);
						}
						long phaqid = pushUserQueueInfo.getPhaqid();
						int end = pushUserQueueInfo.getEnd();
						long index = pushUserQueueInfo.getIndex();
						if(phaqid == 0 || end == PushUserQueueInfo.end_yes) {
							PushMsgHandleAllQueue pushMsgHandleAllQueue = pushMsgHandleService.nextPushMsgHandleAllQueueItem(phaqid); //获取下一条要推送的通知
							if(null != pushMsgHandleAllQueue) {
								long nextPhaqid = pushMsgHandleAllQueue.getId();
								pushUserQueueInfo.setPhaqid(nextPhaqid);
								pushUserQueueInfo.setIndex(0);
								pushUserQueueInfo.setEnd(PushUserQueueInfo.end_no);
                                pushMsgHandleService.updatePushUserQueueInfo(pushUserQueueInfo);
							}
						} else {
							int handleNum = DistributedConfig.serverNum * baseHandleNum;
							List<PushUserQueue> pushUserQueueItems = pushMsgHandleService.
									getPushUserQueueItems(tableName, index, handleNum); //这次要推送的用户
							int currentSize = pushUserQueueItems.size();
							if(currentSize > 0) {
                                PushMsgHandleAllQueue pushMsgHandleAllQueue = pushMsgHandleService.currentPushMsgHandleAllQueueItem(phaqid);
                                final PushMsg pushMsg = new PushMsg();
                                pushMsg.setId(pushMsgHandleAllQueue.getPushMsgId());
                                pushMsg.setType(PushMsg.type_all);
                                pushMsg.setTitle(pushMsgHandleAllQueue.getTitle());
                                pushMsg.setContent(pushMsgHandleAllQueue.getContent());
                                pushMsg.setExtra(pushMsgHandleAllQueue.getExtra());
                                pushMsg.setPlatform(pushMsgHandleAllQueue.getPlatform());
                                pushMsg.setTime(pushMsgHandleAllQueue.getTime());
//								long payloadId = pushMsgHandleAllQueue.getPayloadId();
//								final Payload payload = pushHandleAllService.loadPayload(payloadId);
								List<Long> uids = Lists.newArrayList();
								for(PushUserQueue pushUserQueueItem : pushUserQueueItems) {
									uids.add(pushUserQueueItem.getUid());
								}
								if(currentSize <= baseHandleNum) {
									handleBatch(uids, pushMsg); //批量推送
								} else {
									List<List<Long>> uidsList = Lists.partition(uids, baseHandleNum);
									for(int i = 0; i < uidsList.size(); i++) {
										final List<Long> subUids = uidsList.get(i);
										completionService.submit(new Callable<Void>() {
											@Override
											public Void call() throws Exception {
												handleBatch(subUids, pushMsg);
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
                            pushMsgHandleService.updatePushUserQueueInfo(pushUserQueueInfo); //更新这次推送的信息
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
