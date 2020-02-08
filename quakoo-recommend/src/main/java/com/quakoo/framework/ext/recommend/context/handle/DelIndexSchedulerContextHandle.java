package com.quakoo.framework.ext.recommend.context.handle;

import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.recommend.distributed.DistributedConfig;
import com.quakoo.framework.ext.recommend.service.RecommendIndexService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Data
public class DelIndexSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(DelIndexSchedulerContextHandle.class);

    private static final int handle_num = 50;

    @Resource
    private RecommendIndexService recommendIndexService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(new Processer());
        thread.start();
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (DistributedConfig.canRunDelIndex) {
                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
                    try {
                        recommendIndexService.handleDelIndex(handle_num);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MINUTES);
                }
            }
        }
    }

}
