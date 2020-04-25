package com.quakoo.framework.ext.recommend.context.handle;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.recommend.distributed.DistributedConfig;
import com.quakoo.framework.ext.recommend.model.SyncInfo;
import com.quakoo.framework.ext.recommend.service.SyncInfoService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class SyncInfoSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(SyncInfoSchedulerContextHandle.class);

    @Resource
    private SyncInfoService syncInfoService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(new Processer());
        thread.start();
    }

    private <T> List<List<T>> partition(List<T> source, int listNum) {
        List<List<T>> res = Lists.newArrayList();
        for (int i = 0; i < listNum; i++) {
            List<T> one = Lists.newArrayList();
            res.add(one);
        }
        for (int i = 0; i < source.size(); i++) {
            int times = i / listNum;
            int listIndex = i - times * listNum;
            res.get(listIndex).add(source.get(i));
        }
        return res;
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (DistributedConfig.serverNum > 0) {
                    try {
                        List<SyncInfo> syncInfos = syncInfoService.getSyncInfos();
                        if (syncInfos.size() > 0) {
                            List<List<SyncInfo>> partitionSyncInfos = partition(syncInfos, DistributedConfig.serverNum);
                            List<SyncInfo> handleSyncInfos = partitionSyncInfos.get(DistributedConfig.serverIndex);
                            int totalSize = 0;
                            for(SyncInfo syncInfo : handleSyncInfos) {
                                long startTime = System.currentTimeMillis();
                                int size = syncInfoService.handle(syncInfo);
                                if(size > 0) logger.info("====== sync " + syncInfo.getEsId() + ", size : " + size + ", time : " + (System.currentTimeMillis() - startTime));
                                totalSize += size;
                            }
                            if(totalSize > 0) Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                            else Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                        } else {
                            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MINUTES);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
                }
            }
        }
    }

}
