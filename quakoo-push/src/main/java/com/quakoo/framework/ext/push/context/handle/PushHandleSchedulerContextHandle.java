package com.quakoo.framework.ext.push.context.handle;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.service.PushMsgHandleService;
import com.quakoo.framework.ext.push.util.SleepUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.transform.ListTransformUtils;
import com.quakoo.baseFramework.transform.ListTransformerStringToLong;
import com.quakoo.framework.ext.push.distributed.DistributedConfig;

/**
 * 推送单个或者批量用户的通知上下文
 * class_name: PushHandleSchedulerContextHandle
 * package: com.quakoo.framework.ext.push.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:13
 **/
public class PushHandleSchedulerContextHandle extends PushBasePushHandleContextHandle {

	Logger logger = LoggerFactory.getLogger(PushHandleSchedulerContextHandle.class);

	private int handle_size = 100;
	
	@Resource
	private PushMsgHandleService pushMsgHandleService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		for(String queueName : pushInfo.push_msg_queue_names) {
			Thread thread = new Thread(new Processer(queueName));
			thread.start();
		}
	}
	
	class Processer implements Runnable {
		
		private String queueName;
		
		public Processer(String queueName) {
			this.queueName = queueName;
		}
		
		@Override
		public void run() {
			while(true) {
				if(DistributedConfig.canRunPushMsgQueue.contains(queueName)) {
					SleepUtils.sleep(10, 1);
					try {
						List<PushMsg> list = pushMsgHandleService.getHandlePushMsgs(queueName, handle_size); //获取要推送的通知
						for(PushMsg pushMsg : list) {
                            if(pushMsg.getType() == PushMsg.type_single) {
                                long uid = pushMsg.getUid();
                                handleSingle(uid, pushMsg); //单个用户通知推送
                            } else {
                                String uidStr = pushMsg.getUids();
                                List<String> strList = Lists.
                                        newArrayList(StringUtils.split(uidStr, ","));
                                List<Long> uids = ListTransformUtils.
                                        transformedList(strList, new ListTransformerStringToLong());
                                handleBatch(uids, pushMsg); //多个用户通知推送
                            }
                        }
                        if(list.size() > 0) pushMsgHandleService.finishHandlePushMsgs(queueName, list); //推送完成更新

//						List<Long> pids = Lists.newArrayList();
//						for(PushHandleQueue one : list) {
//							pids.add(one.getId());
//						}
//						List<Payload> payloads = pushHandleService.getPayloads(pids);
//						Map<Long, Payload> payloadMap = Maps.newHashMap();
//						for(Payload payload : payloads) {
//							if(null != payload)
//								payloadMap.put(payload.getId(), payload);
//						}
//						for(PushHandleQueue one : list) {
//							long pid = one.getId();
//							int type = one.getType();
//							Payload payload = payloadMap.get(pid);
//							if(null != payload) {
//								if(type == PushHandleQueue.type_single) {
//									long uid = one.getUid();
//									handleSingle(uid, payload);
//								} else {
//									String uidStr = one.getUids();
//									List<String> strList = Lists.
//											newArrayList(StringUtils.split(uidStr, ","));
//									List<Long> uids = ListTransformUtils.
//											transformedList(strList, new ListTransformerStringToLong());
//									handleBatch(uids, payload);
//								}
//							}
//						}
//                        if(list.size() > 0) pushHandleService.deleteQueueItems(list);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				} else {
					Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
				}
			}
		}
	}

}
