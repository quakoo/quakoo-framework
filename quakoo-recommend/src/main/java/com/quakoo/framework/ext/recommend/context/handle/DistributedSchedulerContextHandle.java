package com.quakoo.framework.ext.recommend.context.handle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.recommend.distributed.DistributedConfig;
import lombok.Data;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
public class DistributedSchedulerContextHandle extends BaseContextHandle implements DisposableBean {

    Logger logger = LoggerFactory.getLogger(DistributedSchedulerContextHandle.class);

    private static String chatHelpPath = "/recommend_help";

    private String serialNumber; //本服务器序列号

    private CuratorFramework client;

    private PathChildrenCache cached;

    @Override
    public void destroy() throws Exception {
        if(null != this.cached) this.cached.close();
        if(null != this.client) this.client.close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        serialNumber = UUID.randomUUID().toString();
        logger.error("====  serialNumber : " + serialNumber);
        this.initClient();
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while(true) {
                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MINUTES);
                try {
                    List<String> serialNumbers = client.getChildren().forPath(chatHelpPath);
                    serialNumbers = sortSerialNumbers(serialNumbers);
                    handle(serialNumbers);
                } catch (Exception e) {
                }
            }
        }
    }

    private void initClient() throws Exception {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory
                .builder();
        CuratorFramework client = builder
                .connectString(recommendInfo.distributedZkAddress)
                .sessionTimeoutMs(20000)
                .connectionTimeoutMs(5000)
                .canBeReadOnly(false)
                .retryPolicy(
                        new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
                .namespace(recommendInfo.projectName).defaultData(null).build();
        client.start();
        Stat stat=client.checkExists().forPath(chatHelpPath);
        if(null == stat){
            client.create().creatingParentsIfNeeded().
                    withMode(CreateMode.PERSISTENT).forPath(chatHelpPath);
        }
        String path = chatHelpPath + "/" + serialNumber;
        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
        cached = new PathChildrenCache(client, chatHelpPath, true);
        cached.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client,
                                   PathChildrenCacheEvent event) throws Exception {
                PathChildrenCacheEvent.Type eventType = event.getType();
                switch (eventType) {
                    case CONNECTION_RECONNECTED: {
                        cached.rebuild();
                        break;
                    }
                    case CHILD_ADDED: {
                        List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
                        handle(serialNumbers);
                        break;
                    }
                    case CHILD_UPDATED: {
                        List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
                        handle(serialNumbers);
                        break;
                    }
                    case CHILD_REMOVED: {
                        List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
                        handle(serialNumbers);
                        break;
                    }
                    default:
                        break;
                }
            }
        });
        cached.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        List<String> serialNumbers = getSerialNumbers(cached.getCurrentData());
        handle(serialNumbers);
        this.client = client;
    }

    private List<String> getSerialNumbers(List<ChildData> childDatas) {
        List<String> serialNumbers = Lists.newArrayList();
        if (childDatas != null) {
            for (ChildData data : childDatas) {
                String path = data.getPath();
                serialNumbers.add(path.substring(path.lastIndexOf("/")+1));
            }
        }
        return sortSerialNumbers(serialNumbers);
    }

    private List<String> sortSerialNumbers(List<String> serialNumbers) {
        int length = serialNumber.length();
        Map<Long, String> map = Maps.newTreeMap();
        for(String serialNumber : serialNumbers) {
            long sort = Long.parseLong(serialNumber.substring(length));
            map.put(sort, serialNumber);
        }
        return Lists.newArrayList(map.values());
    }

    private void handle(List<String> serialNumbers) {
        if(serialNumbers.size() > 0) {
            int serverNum = serialNumbers.size();
            int serverIndex = -1;
            for(int i = 0; i < serverNum; i++) {
                if(serialNumbers.get(i).startsWith(serialNumber)){
                    serverIndex = i;
                    break;
                }
            }
            if(serverIndex >= 0) {
                DistributedConfig.serverNum = serverNum;
                DistributedConfig.serverIndex = serverIndex;

                if(serverIndex == 0) {
                    DistributedConfig.canRunIDFMissWord = true;
                } else {
                    DistributedConfig.canRunIDFMissWord = false;
                }

                if(serverIndex == (serverNum - 1)) {
                    DistributedConfig.canRunHotWord = true;
                    DistributedConfig.canRunPortrait = true;
                } else {
                    DistributedConfig.canRunHotWord = false;
                    DistributedConfig.canRunPortrait = false;
                }
            }
        }
    }

}
