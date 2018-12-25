package com.quakoo.framework.ext.push.context.handle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.Payload;
import com.quakoo.framework.ext.push.model.PushHandleQueue;
import com.quakoo.framework.ext.push.util.SleepUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.transform.ListTransformUtils;
import com.quakoo.baseFramework.transform.ListTransformerStringToLong;
import com.quakoo.framework.ext.push.distributed.DistributedConfig;
import com.quakoo.framework.ext.push.service.PushHandleService;

public class PushHandleSchedulerContextHandle extends PushBasePushHandleContextHandle {

	Logger logger = LoggerFactory.getLogger(PushHandleSchedulerContextHandle.class);

	private int handle_size = 10;
	
	@Resource
	private PushHandleService pushHandleService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		for(String tableName : pushInfo.push_handle_queue_table_names) {
			Thread thread = new Thread(new Processer(tableName));
			thread.start();
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
				if(DistributedConfig.canRunHandleQueueTable.contains(tableName)) {
					SleepUtils.sleep(200, 50);
					try {
						List<PushHandleQueue> list = pushHandleService.
								getHandleQueueItems(tableName, handle_size);
						List<Long> pids = Lists.newArrayList();
						for(PushHandleQueue one : list) {
							pids.add(one.getPayloadId());
						}
						List<Payload> payloads = pushHandleService.getPayloads(pids);
						Map<Long, Payload> payloadMap = Maps.newHashMap();
						for(Payload payload : payloads) {
							if(null != payload) 
								payloadMap.put(payload.getId(), payload);
						}
						for(PushHandleQueue one : list) {
							long pid = one.getPayloadId();
							int type = one.getType();
							Payload payload = payloadMap.get(pid);
							if(null != payload) {
								if(type == PushHandleQueue.type_single) {
									long uid = one.getUid();
									handleSingle(uid, payload);
								} else {
									String uidStr = one.getUids();
									List<String> strList = Lists.
											newArrayList(StringUtils.split(uidStr, ","));
									List<Long> uids = ListTransformUtils.
											transformedList(strList, new ListTransformerStringToLong());
									handleBatch(uids, payload);
								}
							}
							pushHandleService.deleteQueueItem(one);
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
