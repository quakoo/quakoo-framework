package com.quakoo.framework.ext.chat.context.handle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.ChatGroup;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.service.ChatGroupService;
import com.quakoo.framework.ext.chat.service.ManyChatQueueService;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.UserStreamService;

public class ManyChatSchedulerContextHandle extends BaseContextHandle {

	Logger logger = LoggerFactory.getLogger(ManyChatSchedulerContextHandle.class);

	private int handle_size = 5;

	@Resource
	private ManyChatQueueService manyChatQueueService;

	@Resource
	private UserDirectoryService userDirectoryService;

	@Resource
	private UserStreamService userStreamService;

	@Resource
	private ChatGroupService chatGroupService;

	@Override
	public void afterPropertiesSet() throws Exception {
		for (String tableName : chatInfo.many_chat_queue_table_names) {
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
			while (true) {
				if(DistributedConfig.canRunManyChatTable.contains(tableName)) {
					SleepUtils.sleep(200, 50);
					try {
						boolean sign = manyChatQueueService
								.unfinishedIsNull(tableName);
						if (!sign) {
							List<ManyChatQueue> list = manyChatQueueService
									.unfinishedList(tableName, handle_size);
							if (null != list && list.size() > 0) {
								Set<UserDirectory> directories = Sets.newHashSet();
								List<UserStream> streams = Lists.newArrayList();
								for (ManyChatQueue one : list) {
									long uid = one.getUid();
									long cgid = one.getCgid();
									long mid = one.getMid();
									long time = System.currentTimeMillis();

									ChatGroup chatGroup = chatGroupService
											.load(cgid);
									List<Long> uids = JsonUtils.fromJson(
											chatGroup.getUids(),
											new TypeReference<List<Long>>() {
											});
									for (long oneUid : uids) {
										UserDirectory directory = new UserDirectory();
										directory.setUid(oneUid);
										directory.setThirdId(cgid);
										directory.setType(Type.type_many_chat);
										directory.setCtime(time);
										directories.add(directory);

										UserStream stream = new UserStream();
										stream.setUid(oneUid);
										stream.setThirdId(cgid);
										stream.setType(Type.type_many_chat);
										stream.setMid(mid);
										stream.setAuthorId(uid);
										streams.add(stream);
									}
								}
                                if (directories.size() > 0) {
                                    List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories));
                                    if(directoryList.size() > 0)  userDirectoryService.batchInsert(Lists.newArrayList(directories));
                                }
								if (streams.size() > 0)
									userStreamService.batchInsert(streams);
                                if(list.size() > 0) manyChatQueueService.updateStatus(list, Status.finished);
//								for (ManyChatQueue one : list) {
//									manyChatQueueService.updateStatus(one,
//											Status.finished);
//								}
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
