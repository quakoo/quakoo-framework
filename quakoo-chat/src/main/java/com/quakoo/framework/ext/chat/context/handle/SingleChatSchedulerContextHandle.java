package com.quakoo.framework.ext.chat.context.handle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.service.SingleChatQueueService;
import com.quakoo.framework.ext.chat.service.UserDirectoryService;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import com.quakoo.framework.ext.chat.util.SleepUtils;

/**
 * 单聊消息上下文(用于发送单人消息)
 * <p>
 * class_name: SingleChatSchedulerContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:45
 **/
public class SingleChatSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(SingleChatSchedulerContextHandle.class);

    private int handle_size = 20; //批量处理条数
    private int batch_inset_size = 50;

    @Resource
    private SingleChatQueueService singleChatQueueService;

    @Resource
    private UserDirectoryService userDirectoryService;

    @Resource
    private UserStreamService userStreamService;

    @Override
    public void afterPropertiesSet() throws Exception {
//		for(String tableName : chatInfo.single_chat_queue_table_names) {
//			Thread thread = new Thread(new Processer(tableName));
//			thread.start();
//		}
        for (String queueName : chatInfo.single_chat_queue_names) {
            Thread thread = new Thread(new Processer(queueName));
            thread.start();
        }
    }

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
                if (DistributedConfig.canRunSingleQueue.contains(queueName)) {
                    SleepUtils.sleep(200, 50);
                    try {
                        List<SingleChatQueue> list = singleChatQueueService.list(queueName, handle_size); //获取待操作的单人消息列表
                        if (null != list && list.size() > 0) {
                            Set<UserDirectory> directories = Sets.newHashSet();
                            List<UserStream> streams = Lists.newArrayList();
                            for (SingleChatQueue one : list) {
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
//                                userStreamService.batchInsert(streams); //批量添加用户消息流
                            }
                            singleChatQueueService.delete(list);
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
//                if (DistributedConfig.canRunSingleChatTable.contains(tableName)) {
//                    SleepUtils.sleep(200, 50);
//                    try {
//                        boolean sign = singleChatQueueService.unfinishedIsNull(tableName);
//                        if (!sign) {
//                            List<SingleChatQueue> list = singleChatQueueService.unfinishedList(tableName, handle_size); //获取待操作的单人消息列表
//                            if (null != list && list.size() > 0) {
//                                Set<UserDirectory> directories = Sets.newHashSet();
//                                List<UserStream> streams = Lists.newArrayList();
//                                for (SingleChatQueue one : list) {
//                                    long uid = one.getUid();
//                                    long toUid = one.getToUid();
//                                    long mid = one.getMid();
//                                    long time = System.currentTimeMillis();
//
//                                    UserDirectory oneDirectory = new UserDirectory();
//                                    oneDirectory.setUid(uid);
//                                    oneDirectory.setThirdId(toUid);
//                                    oneDirectory.setType(Type.type_single_chat);
//                                    oneDirectory.setCtime(time);
//                                    directories.add(oneDirectory);
//
//                                    UserDirectory twoDirectory = new UserDirectory();
//                                    twoDirectory.setUid(toUid);
//                                    twoDirectory.setThirdId(uid);
//                                    twoDirectory.setType(Type.type_single_chat);
//                                    twoDirectory.setCtime(time);
//                                    directories.add(twoDirectory);
//
//                                    UserStream oneStream = new UserStream();
//                                    oneStream.setUid(uid);
//                                    oneStream.setThirdId(toUid);
//                                    oneStream.setType(Type.type_single_chat);
//                                    oneStream.setMid(mid);
//                                    oneStream.setAuthorId(uid);
//                                    streams.add(oneStream);
//
//                                    UserStream twoStream = new UserStream();
//                                    twoStream.setUid(toUid);
//                                    twoStream.setThirdId(uid);
//                                    twoStream.setType(Type.type_single_chat);
//                                    twoStream.setMid(mid);
//                                    twoStream.setAuthorId(uid);
//                                    streams.add(twoStream);
//                                }
//                                if (directories.size() > 0) {
//                                    List<UserDirectory> directoryList = userDirectoryService.filterExists(Lists.newArrayList(directories)); //过滤用户消息目录
//                                    if (directoryList.size() > 0)
//                                        userDirectoryService.batchInsert(Lists.newArrayList(directories)); //如果有新的聊天目录则批量添加
//                                }
//                                if (streams.size() > 0)
//                                    userStreamService.batchInsert(streams);  //批量添加用户消息流
//                                if (list.size() > 0)
//                                    singleChatQueueService.updateStatus(list, Status.finished); //更新单人消息处理队列
////								for(SingleChatQueue one : list) {
////									singleChatQueueService.updateStatus(one, Status.finished);
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
