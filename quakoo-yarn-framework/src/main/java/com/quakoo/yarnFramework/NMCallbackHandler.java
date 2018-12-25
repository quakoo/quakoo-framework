package com.quakoo.yarnFramework;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by 136249 on 2015/3/10.
 */
public class NMCallbackHandler implements NMClientAsync.CallbackHandler {
    Logger logger = LoggerFactory.getLogger(NMCallbackHandler.class);

    private ConcurrentMap<ContainerId, Container> containers = new ConcurrentHashMap<>();
    private final AbstractAm am;

    public NMCallbackHandler(AbstractAm am) {
        this.am = am;
    }


    public void addContainer(ContainerId containerId, Container container) {
        //6.容器启动前增加容器
        containers.putIfAbsent(containerId, container);
    }

    @Override
    public void onContainerStarted(ContainerId containerId,
                                   Map<String, ByteBuffer> allServiceResponse) {
        //7.容器启动
        logger.info("succeeded to start Container " + containerId);
        Container container = containers.get(containerId);
        if (container != null) {
            //获取容器状态
            am.nmClientAsync.getContainerStatusAsync(containerId, container.getNodeId());
        }
        //发送消息记录
        am.publishContainerStartEvent(am.getAmContext().getTimelineClient(), container,
                am.domainId, am.appSubmitterUgi);
    }

    @Override
    public void onGetContainerStatusError(
            ContainerId containerId, Throwable t) {
        //获取容器状态失败
        logger.error("Failed to query the status of Container " + containerId);
    }

    @Override
    public void onContainerStopped(ContainerId containerId) {
        //8.容器停止
        logger.info("Succeeded to stop Container " + containerId);
        containers.remove(containerId);
    }

    @Override
    public void onContainerStatusReceived(ContainerId containerId,
                                          ContainerStatus containerStatus) {
        logger.info("Container Status: id=" + containerId + ", status=" +
                containerStatus);
    }


    @Override
    public void onStartContainerError(ContainerId containerId, Throwable t) {
        logger.error("Failed to start Container " + containerId);
        containers.remove(containerId);
        am.numCompletedContainers.incrementAndGet();
        am.numFailedContainers.incrementAndGet();
    }


    @Override
    public void onStopContainerError(ContainerId containerId, Throwable t) {
        logger.info("Failed to stop Container " + containerId);
        containers.remove(containerId);
    }
}