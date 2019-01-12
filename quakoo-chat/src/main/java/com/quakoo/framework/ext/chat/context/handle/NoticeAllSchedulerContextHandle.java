package com.quakoo.framework.ext.chat.context.handle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.NoticeAllQueue;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.service.NoticeAllQueueService;
import com.quakoo.framework.ext.chat.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.UserInfoService;
import com.quakoo.framework.ext.chat.service.UserStreamService;

public class NoticeAllSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(NoticeAllSchedulerContextHandle.class);
	
	private int handle_size = 1;
	
	private int scan_size = 200;
	
	@Resource
	private NoticeAllQueueService noticeAllQueueService;
	
	@Resource
	private UserDirectoryService userDirectoryService;
	
	@Resource
	private UserStreamService userStreamService;
	
	@Resource
	private UserInfoService userInfoService;

	@Override
	public void afterPropertiesSet() throws Exception {
		Thread thread = new Thread(new Processer());
		thread.start();
	}
	
	class Processer implements Runnable {
		@Override
		public void run() {
			while(true) {
				if(DistributedConfig.canRunNoticeAll) {
					SleepUtils.sleep(200, 50);
					try {
						boolean sign = noticeAllQueueService.unfinishedIsNull();
						if(!sign) {
							List<NoticeAllQueue> list = noticeAllQueueService.unfinishedList(handle_size);
							if(null != list && list.size() > 0) {
								for(NoticeAllQueue one : list) {
									CompletionService<Void> completionService = 
											new ExecutorCompletionService<Void>(Executors.newCachedThreadPool());
									for(String tableName : chatInfo.user_info_table_names) {
										UserInfoScan scan = new UserInfoScan(tableName, one, scan_size);
										completionService.submit(scan);
									}
									for(int i = 0; i < chatInfo.user_info_table_names.size(); i++) {
										completionService.take().get();
									}
									noticeAllQueueService.updateStatus(one, Status.finished);
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
		
		class UserInfoScan implements Callable<Void> {
			
            private String tableName;
			
			private NoticeAllQueue notice;
			
			private int scanSize;
			
			public UserInfoScan(String tableName, NoticeAllQueue notice, int scanSize) {
				this.tableName = tableName;
				this.notice = notice;
				this.scanSize = scanSize;
			}

			@Override
			public Void call() throws Exception {
				long mid = notice.getMid();
				long time = System.currentTimeMillis();
				long authorId = notice.getAuthorId();
				
				int currentSize = 0;
				double lastTime = Double.MAX_VALUE;
				do {
					List<UserInfo> list = userInfoService.list(tableName, lastTime, scanSize + 1);
					currentSize = list.size();
					int num = currentSize > scanSize ? scanSize : currentSize;
					Set<UserDirectory> directories = Sets.newHashSet();
					List<UserStream> streams = Lists.newArrayList();
					for(int i = 0; i < num; i++) {
						UserInfo userInfo = list.get(i);
						long uid = userInfo.getUid();
						UserDirectory directory = new UserDirectory();
						directory.setUid(uid);
						directory.setThirdId(authorId);
						directory.setType(Type.type_notice);
						directory.setCtime(time);
						directories.add(directory);
						
						UserStream stream = new UserStream();
						stream.setUid(uid);
						stream.setThirdId(authorId);
						stream.setMid(mid);
						stream.setType(Type.type_notice);
						stream.setAuthorId(authorId);
						streams.add(stream);
					}
                    if(directories.size() > 0) {
                        List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories));
                        if(directoryList.size() > 0)  userDirectoryService.batchInsert(Lists.newArrayList(directories));
                    }
					if(streams.size() > 0)
						userStreamService.batchInsert(streams);
					if(currentSize > scanSize) lastTime = list.get(list.size() - 1).getLoginTime();
				} while(currentSize > scanSize);
				return null;
			}
		}
	}

}
