package com.quakoo.framework.ext.chat.context.handle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.service.SingleChatQueueService;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.UserStreamService;

public class SingleChatSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(SingleChatSchedulerContextHandle.class);
	
	private int handle_size = 10;
	
	@Resource
	private SingleChatQueueService singleChatQueueService;
	
	@Resource
	private UserDirectoryService userDirectoryService;
	
	@Resource
	private UserStreamService userStreamService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		for(String tableName : chatInfo.single_chat_queue_table_names) {
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
				if(DistributedConfig.canRunSingleChatTable.contains(tableName)) {
					SleepUtils.sleep(200, 50);
					try {
						boolean sign = singleChatQueueService.unfinishedIsNull(tableName);
						if(!sign){
							List<SingleChatQueue> list = singleChatQueueService.unfinishedList(tableName, handle_size);
							if(null != list && list.size() > 0) {
								Set<UserDirectory> directories = Sets.newHashSet();
								List<UserStream> streams = Lists.newArrayList();
								for(SingleChatQueue one : list) {
									long uid = one.getUid();
									long toUid = one.getToUid();
									long mid = one.getMid();
									long time = System.currentTimeMillis();
									
									UserDirectory oneDirectory = new UserDirectory();
									oneDirectory.setUid(uid);
									oneDirectory.setThirdId(toUid);
									oneDirectory.setType(Type.type_single_chat);
									oneDirectory.setCtime(time);
									directories.add(oneDirectory);
									
									UserDirectory twoDirectory = new UserDirectory();
									twoDirectory.setUid(toUid);
									twoDirectory.setThirdId(uid);
									twoDirectory.setType(Type.type_single_chat);
									twoDirectory.setCtime(time);
									directories.add(twoDirectory);
									
									UserStream oneStream = new UserStream();
									oneStream.setUid(uid);
									oneStream.setThirdId(toUid);
									oneStream.setType(Type.type_single_chat);
									oneStream.setMid(mid);
									oneStream.setAuthorId(uid);
									streams.add(oneStream);
									
									UserStream twoStream = new UserStream();
									twoStream.setUid(toUid);
									twoStream.setThirdId(uid);
									twoStream.setType(Type.type_single_chat);
									twoStream.setMid(mid);
									twoStream.setAuthorId(uid);
									streams.add(twoStream);
								}
                                if(directories.size() > 0) {
                                    List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories));
                                    if(directoryList.size() > 0)  userDirectoryService.batchInsert(Lists.newArrayList(directories));
                                }
								if(streams.size() > 0)
									userStreamService.batchInsert(streams);
								for(SingleChatQueue one : list) {
									singleChatQueueService.updateStatus(one, Status.finished);
								}
							}
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
