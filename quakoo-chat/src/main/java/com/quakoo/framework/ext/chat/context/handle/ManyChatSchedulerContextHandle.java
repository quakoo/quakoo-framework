package com.quakoo.framework.ext.chat.context.handle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.ChatGroup;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.service.ChatGroupService;
import com.quakoo.framework.ext.chat.service.ManyChatQueueService;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import com.quakoo.framework.ext.chat.util.*;

/**
 * 群聊消息上下文(用于发送多人消息)
 * <p>
 * class_name: ManyChatSchedulerContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:40
 **/
public class ManyChatSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(ManyChatSchedulerContextHandle.class);

    private int handle_size = 5; //批量处理条数
    private int batch_inset_size = 50;

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
        for (String queueName : chatInfo.many_chat_queue_names) {
            Thread thread = new Thread(new Processer(queueName));
            thread.start();
        }
    }

//	public void afterPropertiesSet() throws Exception {
//		for (String tableName : chatInfo.many_chat_queue_table_names) {
//			Thread thread = new Thread(new Processer(tableName));
//			thread.start();
//		}
//	}


    /**
     * 处理线程
     * class_name: ManyChatSchedulerContextHandle
     * package: com.quakoo.framework.ext.chat.context.handle
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 16:41
     **/
    class Processer implements Runnable {

        //        private String tableName;
        private String queueName;

        //        public Processer(String tableName) {
//            this.tableName = tableName;
//        }
        public Processer(String queueName) {
            this.queueName = queueName;
        }


        @Override
        public void run() {
            while (true) {
                if (DistributedConfig.canRunManyQueue.contains(queueName)) {
                    SleepUtils.sleep(200, 50);
                    try {
                        List<ManyChatQueue> list = manyChatQueueService.list(queueName, handle_size); //获取待操作的多人消息列表
                        if (null != list && list.size() > 0) {
                            Set<UserDirectory> directories = Sets.newHashSet();
                            List<UserStream> streams = Lists.newArrayList();
                            for (ManyChatQueue one : list) {
                                long uid = one.getUid();
                                long cgid = one.getCgid();
                                long mid = one.getMid();
                                long time = System.currentTimeMillis();

                                ChatGroup chatGroup = chatGroupService.load(cgid); //获取群组所有的用户
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
                                List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories)); //过滤用户消息目录
                                if (directoryList.size() > 0)
                                    userDirectoryService.batchInsert(Lists.newArrayList(directories)); //如果有新的聊天目录则批量添加
                            }
                            if (streams.size() > 0) {
                                if (streams.size() <= batch_inset_size) {
                                    userStreamService.batchInsert(streams); //批量添加用户消息流
                                } else {
                                    List<List<UserStream>> streamsList = Lists.partition(streams, batch_inset_size);
                                    for (List<UserStream> one : streamsList) {
                                        userStreamService.batchInsert(one); //批量添加用户消息流
                                    }
                                }
//                              userStreamService.batchInsert(streams); //批量添加用户消息流
                            }
                            manyChatQueueService.delete(list);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
                }
            }
        }

//        @Override
//        public void run() {
//            while (true) {
//                if (DistributedConfig.canRunManyChatTable.contains(tableName)) {
//                    SleepUtils.sleep(200, 50);
//                    try {
//                        boolean sign = manyChatQueueService
//                                .unfinishedIsNull(tableName);
//                        if (!sign) {
//                            List<ManyChatQueue> list = manyChatQueueService.unfinishedList(tableName, handle_size); //获取待操作的多人消息列表
//                            if (null != list && list.size() > 0) {
//                                Set<UserDirectory> directories = Sets.newHashSet();
//                                List<UserStream> streams = Lists.newArrayList();
//                                for (ManyChatQueue one : list) {
//                                    long uid = one.getUid();
//                                    long cgid = one.getCgid();
//                                    long mid = one.getMid();
//                                    long time = System.currentTimeMillis();
//
//                                    ChatGroup chatGroup = chatGroupService.load(cgid); //获取群组所有的用户
//                                    List<Long> uids = JsonUtils.fromJson(
//                                            chatGroup.getUids(),
//                                            new TypeReference<List<Long>>() {
//                                            });
//                                    for (long oneUid : uids) {
//                                        UserDirectory directory = new UserDirectory();
//                                        directory.setUid(oneUid);
//                                        directory.setThirdId(cgid);
//                                        directory.setType(Type.type_many_chat);
//                                        directory.setCtime(time);
//                                        directories.add(directory);
//
//                                        UserStream stream = new UserStream();
//                                        stream.setUid(oneUid);
//                                        stream.setThirdId(cgid);
//                                        stream.setType(Type.type_many_chat);
//                                        stream.setMid(mid);
//                                        stream.setAuthorId(uid);
//                                        streams.add(stream);
//                                    }
//                                }
//                                if (directories.size() > 0) {
//                                    List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories)); //过滤用户消息目录
//                                    if (directoryList.size() > 0)
//                                        userDirectoryService.batchInsert(Lists.newArrayList(directories)); //如果有新的聊天目录则批量添加
//                                }
//                                if (streams.size() > 0)
//                                    userStreamService.batchInsert(streams); //批量添加用户消息流
//                                if (list.size() > 0)
//                                    manyChatQueueService.updateStatus(list, Status.finished); //更新多人消息处理队列
////								for (ManyChatQueue one : list) {
////									manyChatQueueService.updateStatus(one, Status.finished);
////								}
//                            }
//                        }
//                    } catch (Exception e) {
//                        logger.error(e.getMessage(), e);
//                    }
//                } else {
//                    Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
//                }
//            }
//        }

    }

}
