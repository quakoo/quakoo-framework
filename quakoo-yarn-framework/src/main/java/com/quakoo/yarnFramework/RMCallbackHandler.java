package com.quakoo.yarnFramework;

import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by 136249 on 2015/3/10.
 */
public class RMCallbackHandler implements AMRMClientAsync.CallbackHandler {

    Logger logger = LoggerFactory.getLogger(RMCallbackHandler.class);

    private final AbstractAm am;
    private final AmContext amContext;

    public RMCallbackHandler(AbstractAm applicationMaster) {
        this.am = applicationMaster;
        this.amContext = am.getAmContext();
    }

    @Override
    public void onContainersAllocated(List<Container> allocatedContainers) {
        //4.得到申请到的容器
        logger.info("Got response from RM for container ask, allocatedCnt="
                + allocatedContainers.size());
        am.numAllocatedContainers.addAndGet(allocatedContainers.size());//申请到的容器数+1
        for (Container allocatedContainer : allocatedContainers) {
            logger.info("Launching shell command on a new container."
                    + ", containerId=" + allocatedContainer.getId()
                    + ", containerNode=" + allocatedContainer.getNodeId().getHost()
                    + ":" + allocatedContainer.getNodeId().getPort()
                    + ", containerNodeURI=" + allocatedContainer.getNodeHttpAddress()
                    + ", containerResourceMemory"
                    + allocatedContainer.getResource().getMemory()
                    + ", containerResourceVirtualCores"
                    + allocatedContainer.getResource().getVirtualCores());
            LinkedBlockingQueue<Map<String, String>> blockingQueue=am.getAmContext().getClientTaskRequest().getTaskParams();
            Map<String, String> taskParams=blockingQueue.poll();
            am.getAmContext().getTaskContext().put(allocatedContainer.getId(), taskParams);
            //5.启动容器
            LaunchContainerRunnable runnableLaunchContainer =
                    new LaunchContainerRunnable(allocatedContainer, am.containerListener, am,taskParams);

            Thread launchThread = new Thread(runnableLaunchContainer);
            am.launchThreads.add(launchThread);
            launchThread.start();
        }



    }

    @SuppressWarnings("unchecked")
    @Override
    public void onContainersCompleted(List<ContainerStatus> completedContainers) {
        //9.容器执行完毕
        logger.info("Got response from RM for container ask, completedCnt="
                + completedContainers.size());
        for (ContainerStatus containerStatus : completedContainers) {
            logger.info(amContext.getAppAttemptID() + " got container status for containerID="
                    + containerStatus.getContainerId() + ", state="
                    + containerStatus.getState() + ", exitStatus="
                    + containerStatus.getExitStatus() + ", diagnostics="
                    + containerStatus.getDiagnostics());

            // non complete containers should not be here
            assert (containerStatus.getState() == ContainerState.COMPLETE);

            // increment counters for completed/failed containers
            int exitStatus = containerStatus.getExitStatus();
            //查看退出的状态号码
            if (0 != exitStatus) {
                //该容器不是被框架干死的。
                if (ContainerExitStatus.ABORTED != exitStatus) {
                    //其他的各种错误吧。。。
                    am.numCompletedContainers.incrementAndGet();//完成+1
                    am.numFailedContainers.incrementAndGet();//失败+1
                } else {
                    //该容器被框架干死了。
                    am.numAllocatedContainers.decrementAndGet();//申请到的容器数-1
                    am.numRequestedContainers.decrementAndGet();//提交申请的容器-1
                    // we do not need to release the container as it would be done
                    // by the RM

                    //重启任务
                    Map<String,String> params=am.getAmContext().getTaskContext().get(containerStatus.getContainerId());
                    if(params!=null) {
                        am.getAmContext().getClientTaskRequest().getTaskParams().add(params);
                    }
                    logger.info("need re allocatedContainer"+amContext.getAppAttemptID() + " got container status for containerID="
                            + containerStatus.getContainerId() + ", state="
                            + containerStatus.getState() + ", exitStatus="
                            + containerStatus.getExitStatus() + ", diagnostics="
                            + containerStatus.getDiagnostics());
                }
            } else {
                am.numCompletedContainers.incrementAndGet();
                logger.info("Container completed successfully." + ", containerId="
                        + containerStatus.getContainerId());
            }
            am.publishContainerEndEvent(
                    amContext.getTimelineClient(), containerStatus, am.domainId, am.appSubmitterUgi);
        }


        //检查是否需要继续申请容器(需求的容器数-提交申请的容器数)
        int askCount = amContext.getClientTaskRequest().getNumContainers() - am.numRequestedContainers.get();
        am.numRequestedContainers.addAndGet(askCount);

        if (askCount > 0) {
            for (int i = 0; i < askCount; ++i) {
                ContainerRequest containerAsk = am.setupContainerAskForRM();
                am.amRMClient.addContainerRequest(containerAsk);
            }
        }
        //如果该任务申请的所有的容器都执行完毕，标志任务已经执行完成
        if (am.numCompletedContainers.get() == amContext.getClientTaskRequest().getNumContainers()) {
            am.done = true;
        }
    }

    @Override
    public void onShutdownRequest() {
        am.done = true;
    }

    @Override
    public void onNodesUpdated(List<NodeReport> updatedNodes) {
    }

    @Override
    public float getProgress() {
        // set progress to deliver to RM on next heartbeat
        float progress = (float) am.numCompletedContainers.get()
                / amContext.getClientTaskRequest().getNumContainers();
        return progress;
    }

    @Override
    public void onError(Throwable e) {
        am.done = true;
        am.amRMClient.stop();
    }
}