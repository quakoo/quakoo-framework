package com.quakoo.framework.ext.chat.context.handle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.UserStreamQueue;
import com.quakoo.framework.ext.chat.service.UserStreamQueueService;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserStreamSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(UserStreamSchedulerContextHandle.class);

    private int handle_size = 10; //批量处理条数

    @Resource
    private UserStreamQueueService userStreamQueueService;

    @Resource
    private UserStreamService userStreamService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(new Processer());
        thread.start();
    }


    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                for (String queueName : chatInfo.user_stream_queue_names) {
                    if (DistributedConfig.canRunUserStreamQueue.contains(queueName)) {
                        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                        try {
                            List<UserStreamQueue> list = userStreamQueueService.list(queueName, handle_size);
                            if (null != list && list.size() > 0) {
                                List<UserStream> userStreams = Lists.newArrayList();
                                Map<Long, Double> uidSortMap = Maps.newHashMap();
                                for (UserStreamQueue one : list) {
                                    UserStream userStream = new UserStream();
                                    userStream.setUid(one.getUid());
                                    userStream.setType(one.getType());
                                    userStream.setThirdId(one.getThirdId());
                                    userStream.setAuthorId(one.getAuthorId());
                                    userStream.setMid(one.getMid());
                                    userStream.setSort(one.getSort());
                                    userStreams.add(userStream);
                                    Double sort = uidSortMap.get(one.getUid());
                                    if(null == sort) {
                                        uidSortMap.put(one.getUid(), one.getSort());
                                    } else {
                                        if(sort < one.getSort()) uidSortMap.put(one.getUid(), one.getSort());
                                    }
                                }
                                userStreamService.batchInsertColdData(userStreams); //插入冷数据
                                userStreamQueueService.delete(list);

                                for(Map.Entry<Long, Double> entry : uidSortMap.entrySet()) {
                                    long uid = entry.getKey();
                                    double sort = entry.getValue();
                                    userStreamService.clearHotData(uid, sort); //清除热数据
                                }
                            }

                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

}
