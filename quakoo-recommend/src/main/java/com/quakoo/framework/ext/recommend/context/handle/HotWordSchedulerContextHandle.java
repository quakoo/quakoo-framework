package com.quakoo.framework.ext.recommend.context.handle;

import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.recommend.distributed.DistributedConfig;
import com.quakoo.framework.ext.recommend.service.HotWordService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Data
public class HotWordSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(IDFMissWordSchedulerContextHandle.class);

    @Resource
    private HotWordService hotWordService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(new Processer());
        thread.start();
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (DistributedConfig.canRunHotWord) {
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MINUTES);
                    try {
                        hotWordService.handle(new Date());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.MINUTES);
                }
            }
        }
    }

}
