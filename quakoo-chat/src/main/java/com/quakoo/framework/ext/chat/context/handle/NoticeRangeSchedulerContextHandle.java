package com.quakoo.framework.ext.chat.context.handle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.model.*;
import com.quakoo.framework.ext.chat.service.UserStreamQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.service.NoticeRangeQueueService;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import com.quakoo.framework.ext.chat.util.SleepUtils;

/**
 * 发送一批人通知
 * class_name: NoticeRangeSchedulerContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:44
 **/
public class NoticeRangeSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(NoticeRangeSchedulerContextHandle.class);
 	
	private int handle_size = 10;
	
	@Resource
	private NoticeRangeQueueService noticeRangeQueueService;
	
	@Resource
	private UserDirectoryService userDirectoryService;
	
	@Resource
	private UserStreamService userStreamService;

	@Resource
	private UserStreamQueueService userStreamQueueService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Thread thread = new Thread(new Processer());
		thread.start();
	}
	
	class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                if(DistributedConfig.canRunNoticeRange) {
                    SleepUtils.sleep(200, 50);
                    try {
                        List<NoticeRangeQueue> list = noticeRangeQueueService.list(handle_size); //获取待操作的多人消息列表
                        if (null != list && list.size() > 0) {
                            Set<UserDirectory> directories = Sets.newHashSet();
                            List<UserStream> streams = Lists.newArrayList();
                            for (NoticeRangeQueue one : list) {
                                long mid = one.getMid();
                                long time = System.currentTimeMillis();
                                long authorId = one.getAuthorId();
                                List<Long> uids = JsonUtils.fromJson(one.getUids(), new TypeReference<List<Long>>() {});
                                for (long oneUid : uids) {
                                    UserDirectory directory = new UserDirectory();
                                    directory.setUid(oneUid);
                                    directory.setThirdId(authorId);
                                    directory.setType(Type.type_notice);
                                    directory.setCtime(time);
                                    directories.add(directory);

                                    UserStream stream = new UserStream();
                                    stream.setUid(oneUid);
                                    stream.setThirdId(authorId);
                                    stream.setMid(mid);
                                    stream.setType(Type.type_notice);
                                    stream.setAuthorId(authorId);
                                    streams.add(stream);
                                }
                            }
                            if (directories.size() > 0) {
                                List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories));
                                if (directoryList.size() > 0)
                                    userDirectoryService.batchInsert(Lists.newArrayList(directories));
                            }
                            if (streams.size() > 0) {
                                userStreamService.createSort(streams); //批量生成sort字段
                                userStreamService.batchInsertHotData(streams); //批量插入热数据
                                List<UserStreamQueue> streamQueues = Lists.newArrayList();
                                for(UserStream one : streams) {
                                    UserStreamQueue streamQueue = new UserStreamQueue(one);
                                    streamQueues.add(streamQueue);
                                }
                                userStreamQueueService.insert(streamQueues); //插入待插入队列

//                                userStreamService.batchInsert(streams);
                            }
                            noticeRangeQueueService.delete(list);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
                }
            }
        }

        //		@Override
//		public void run() {
//			while(true) {
//				if(DistributedConfig.canRunNoticeRange) {
//					SleepUtils.sleep(200, 50);
//					try {
//						boolean sign = noticeRangeQueueService.unfinishedIsNull();
//						if(!sign) {
//							List<NoticeRangeQueue> list = noticeRangeQueueService.unfinishedList(handle_size);
//							if(null != list && list.size() > 0) {
//								for(NoticeRangeQueue one : list) {
//									long mid = one.getMid();
//									long time = System.currentTimeMillis();
//									long authorId = one.getAuthorId();
//									List<Long> uids = JsonUtils.fromJson(one.getUids(),
//											new TypeReference<List<Long>>() {});
//									Set<UserDirectory> directories = Sets.newHashSet();
//									List<UserStream> streams = Lists.newArrayList();
//									for(long oneUid : uids) {
//										UserDirectory directory = new UserDirectory();
//										directory.setUid(oneUid);
//										directory.setThirdId(authorId);
//										directory.setType(Type.type_notice);
//										directory.setCtime(time);
//										directories.add(directory);
//
//										UserStream stream = new UserStream();
//										stream.setUid(oneUid);
//										stream.setThirdId(authorId);
//										stream.setMid(mid);
//										stream.setType(Type.type_notice);
//										stream.setAuthorId(authorId);
//										streams.add(stream);
//									}
//									if(directories.size() > 0) {
//                                        List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories));
//                                        if(directoryList.size() > 0)  userDirectoryService.batchInsert(Lists.newArrayList(directories));
//                                    }
//									if(streams.size() > 0)
//										userStreamService.batchInsert(streams);
//									noticeRangeQueueService.updateStatus(one, Status.finished);
//								}
//							}
//						}
//					} catch (Exception e) {
//						logger.error(e.getMessage(), e);
//					}
//				} else {
//					Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
//				}
//			}
//		}
		
	}
	
}
