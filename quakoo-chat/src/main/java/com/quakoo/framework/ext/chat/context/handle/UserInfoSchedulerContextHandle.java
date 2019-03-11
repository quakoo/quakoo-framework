package com.quakoo.framework.ext.chat.context.handle;

import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.service.UserInfoQueueService;
import com.quakoo.framework.ext.chat.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UserInfoSchedulerContextHandle extends BaseContextHandle  {

    Logger logger = LoggerFactory.getLogger(UserInfoSchedulerContextHandle.class);

    private int handle_size = 5; //批量处理条数

    @Resource
    private UserInfoQueueService userInfoQueueService;

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
            while (true) {
                for (String queueName : chatInfo.user_info_queue_names) {
                    if (DistributedConfig.canRunUserInfoQueue.contains(queueName)) {
                        Uninterruptibles.sleepUninterruptibly(3000, TimeUnit.MILLISECONDS);
                        try {
                            List<Long> list = userInfoQueueService.list(queueName, handle_size);
                            if (null != list && list.size() > 0) {
                                List<UserInfo> userInfos = userInfoService.loadCache(list);
                                userInfoService.replace(userInfos);
                                userInfoQueueService.delete(list);
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
