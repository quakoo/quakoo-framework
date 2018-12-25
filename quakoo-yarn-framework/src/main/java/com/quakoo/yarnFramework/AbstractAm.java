package com.quakoo.yarnFramework;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.api.records.timeline.TimelineEntity;
import org.apache.hadoop.yarn.api.records.timeline.TimelineEvent;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.TimelineClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 136249 on 2015/3/10.
 */
public abstract class AbstractAm {
    Logger logger = LoggerFactory.getLogger(AbstractAm.class);

    public static enum DSEvent {
        DS_APP_ATTEMPT_START, DS_APP_ATTEMPT_END, DS_CONTAINER_START, DS_CONTAINER_END
    }

    public static enum DSEntity {
        DS_APP_ATTEMPT, DS_CONTAINER
    }

    public Configuration conf;
    /**
     * am->rm client
     */
    public AMRMClientAsync amRMClient;
    /**
     * nm client
     */
    public NMClientAsync nmClientAsync;
    /**
     * nm listener
     */
    public NMCallbackHandler containerListener;


    public String appMasterHostname = "";
    public int appMasterRpcPort = -1;
    public String appMasterTrackingUrl = "";


    /**
     * 提交申请的容器数目
     */
    protected AtomicInteger numRequestedContainers = new AtomicInteger();

    /**
     * 申请到的容器数目
     */
    public AtomicInteger numAllocatedContainers = new AtomicInteger();

    /**
     * 完成的容器数目（完成和失败的）
     */
    public AtomicInteger numCompletedContainers = new AtomicInteger();

    /**
     * 失败的容器数目
     */
    public AtomicInteger numFailedContainers = new AtomicInteger();

    // Timeline domain ID
    public String domainId = null;


    /**
     * 权限相关
     */
    public ByteBuffer allTokens;
    /**
     * 权限相关
     */
    public UserGroupInformation appSubmitterUgi;

    // Launch threads
    public List<Thread> launchThreads = new ArrayList<>();


    public volatile boolean done;

    private AmContext amContext = new AmContext();

    public AmContext getAmContext() {
        return amContext;
    }

    public void setAmContext(AmContext amContext) {
        this.amContext = amContext;
    }

    public void init(String[] args) throws ParseException, IOException {
        initConf();
        initArgs(args);
    }

    public void initConf() {
        conf = new YarnConfiguration();
    }

    public void initArgs(String[] args) throws ParseException, IOException {
        Options opts = new Options();
        opts.addOption(TaskOnYarnConstans.app_attempt_id, true, "App Attempt ID. Not to be used unless for testing purposes");
        opts.addOption(TaskOnYarnConstans.CLINETTASKREUQESTPARAM, true, "clientTaskRequest");
        CommandLine cliParser = new GnuParser().parse(opts, args);
        String clientTaskRequestStr = cliParser.getOptionValue(TaskOnYarnConstans.CLINETTASKREUQESTPARAM);
        ClientTaskRequest clientTaskRequest = JsonUtils.parse(Base64Util.decode(URLDecoder.decode(clientTaskRequestStr,"utf-8")),
                ClientTaskRequest.class);
        amContext.setClientTaskRequest(clientTaskRequest);
        ApplicationAttemptId appAttemptID;
        Map<String, String> envs = System.getenv();
        if (!envs.containsKey(ApplicationConstants.Environment.CONTAINER_ID.name())) {
            if (cliParser.hasOption(TaskOnYarnConstans.app_attempt_id)) {
                String appIdStr = cliParser.getOptionValue("app_attempt_id", "");
                appAttemptID = ConverterUtils.toApplicationAttemptId(appIdStr);
            } else {
                throw new IllegalArgumentException("Application Attempt Id not set in the environment");
            }
        } else {
            ContainerId containerId = ConverterUtils.toContainerId(envs
                    .get(ApplicationConstants.Environment.CONTAINER_ID.name()));
            appAttemptID = containerId.getApplicationAttemptId();
        }
        amContext.setAppAttemptID(appAttemptID);
        TimelineClient timelineClient = TimelineClient.createTimelineClient();
        timelineClient.init(conf);
        timelineClient.start();
        amContext.setTimelineClient(timelineClient);
    }


    public void run() throws YarnException, IOException {
        String errMsg = null;
        try {
            logger.info("Starting ApplicationMaster");

            Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            Iterator<Token<?>> iter = credentials.getAllTokens().iterator();
            while (iter.hasNext()) {
                Token<?> token = iter.next();
                logger.info("token:" + token);
                if (token.getKind().equals(AMRMTokenIdentifier.KIND_NAME)) {
                    iter.remove();
                }
            }
            allTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
            String appSubmitterUserName = System.getenv(ApplicationConstants.Environment.USER.name());
            appSubmitterUgi = UserGroupInformation.createRemoteUser(appSubmitterUserName);
            appSubmitterUgi.addCredentials(credentials);
            publishApplicationAttemptEvent(amContext.getTimelineClient(), amContext.getAppAttemptID().toString(),
                    DSEvent.DS_APP_ATTEMPT_START, domainId, appSubmitterUgi);

            AMRMClientAsync.CallbackHandler allocListener = new RMCallbackHandler(this);
            amRMClient = AMRMClientAsync.createAMRMClientAsync(TaskOnYarnConstans.rmIntervalMs, allocListener);
            amRMClient.init(conf);
            amRMClient.start();

            containerListener = new NMCallbackHandler(this);
            nmClientAsync = new NMClientAsyncImpl(containerListener);
            nmClientAsync.init(conf);
            nmClientAsync.start();

            appMasterHostname = NetUtils.getHostname();
            //1.对rm 注册am
            RegisterApplicationMasterResponse response = amRMClient.registerApplicationMaster(
                    appMasterHostname, appMasterRpcPort, appMasterTrackingUrl);
            verifyContainerResources(response);
            //2.获取正在运行的容器
            List<Container> previousAMRunningContainers = response.getContainersFromPreviousAttempts();
            logger.info(amContext.getAppAttemptID() + " received " + previousAMRunningContainers.size()
                    + " previous attempts' running containers on AM registration.");
            numAllocatedContainers.addAndGet(previousAMRunningContainers.size());

            //如果容器数目不够
            int numTotalContainersToRequest = amContext.getClientTaskRequest().getNumContainers()
                    - previousAMRunningContainers.size();
            for (int i = 0; i < numTotalContainersToRequest; ++i) {
                AMRMClient.ContainerRequest containerAsk = setupContainerAskForRM();
                //3.增加要申请容器
                amRMClient.addContainerRequest(containerAsk);
            }
            //记录申请容器的数目
            numRequestedContainers.set(amContext.getClientTaskRequest().getNumContainers());
            publishApplicationAttemptEvent(amContext.getTimelineClient(), amContext.getAppAttemptID().toString(),
                    DSEvent.DS_APP_ATTEMPT_END, domainId, appSubmitterUgi);
            errMsg = finish();
        } catch (Throwable t) {
            logger.error("", t);
            ExitUtis.exit(t, 1);
        }
        if (StringUtils.isBlank(errMsg)) {
            logger.info("Application Master completed successfully. exiting");
            System.exit(0);
        } else {
            logger.info("Application Master failed. exiting");
            ExitUtis.exit(errMsg, 1);
        }
    }

    private void verifyContainerResources(RegisterApplicationMasterResponse response) {
        int maxMem = response.getMaximumResourceCapability().getMemory();
        int maxVCores = response.getMaximumResourceCapability().getVirtualCores();
        logger.info("capabililty of resources in this cluster maxVcores:{},maxVCores:{}", maxVCores, maxMem);
        // A resource ask cannot exceed the max.
        if (amContext.getClientTaskRequest().getContainerMemory() > maxMem) {
            logger.info("Container memory specified above max threshold of cluster."
                    + " Using max value." + ", specified=" +
                    amContext.getClientTaskRequest().getContainerMemory() + ", max="
                    + maxMem);
            amContext.getClientTaskRequest().setContainerMemory(maxMem);
        }
        if (amContext.getClientTaskRequest().getContainerVirtualCores() > maxVCores) {
            logger.info("Container virtual cores specified above max threshold of cluster."
                    + " Using max value." +
                    ", specified=" + amContext.getClientTaskRequest().getContainerVirtualCores() + ", max="
                    + maxVCores);
            amContext.getClientTaskRequest().setContainerVirtualCores(maxVCores);
        }
    }


    public String finish() {
        //判断是否完成 是否done,并且完成的容器数和需求的容器数相等
        while (!done && (numCompletedContainers.get() != amContext.getClientTaskRequest().getNumContainers())) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
            }
        }

        //等待子线程完成
        for (Thread launchThread : launchThreads) {
            try {
                launchThread.join(10000);
            } catch (InterruptedException e) {
                logger.info("Exception thrown in thread join: " + e.getMessage());
            }
        }
        logger.info("Application completed. Stopping running containers");
        nmClientAsync.stop();
        logger.info("Application completed. Signalling finish to RM");
        FinalApplicationStatus appStatus;
        String errMsg = null;
        //判断失败的容器数是否等于0，并且是否并且完成的容器数和需求的容器数相等
        if (numFailedContainers.get() == 0 &&
                numCompletedContainers.get() == amContext.getClientTaskRequest().getNumContainers()) {
            appStatus = FinalApplicationStatus.SUCCEEDED;
        } else {
            appStatus = FinalApplicationStatus.FAILED;
            errMsg = "Diagnostics." + ", total=" + amContext.getClientTaskRequest().getNumContainers()
                    + ", completed=" + numCompletedContainers.get() + ", allocated="
                    + numAllocatedContainers.get() + ", failed="
                    + numFailedContainers.get();
            logger.info(errMsg);

        }
        try {
            //unregister ApplicationMaster
            amRMClient.unregisterApplicationMaster(appStatus, errMsg, null);
        } catch (YarnException ex) {
            logger.error("Failed to unregister application", ex);
        } catch (IOException e) {
            logger.error("Failed to unregister application", e);
        }
        amRMClient.stop();
        return errMsg;
    }


    /**
     * Setup the request that will be sent to the RM for the container ask.
     *
     * @return the setup ResourceRequest to be sent to RM
     */
    public AMRMClient.ContainerRequest setupContainerAskForRM() {
        Priority pri = Priority.newInstance(amContext.getClientTaskRequest().getTaskPriority());
        Resource capability = Resource.newInstance(amContext.getClientTaskRequest().getContainerMemory(),
                amContext.getClientTaskRequest().getContainerVirtualCores());
        //申请容器（所需要的资源，节点，机架，优先级）
        AMRMClient.ContainerRequest request = new AMRMClient.ContainerRequest(capability, null, null, pri);
        logger.info("Requested container ask: " + request.toString());
        return request;
    }


    public void publishContainerStartEvent(
            final TimelineClient timelineClient, Container container, String domainId,
            UserGroupInformation ugi) {
        final TimelineEntity entity = new TimelineEntity();
        entity.setEntityId(container.getId().toString());
        entity.setEntityType(DSEntity.DS_CONTAINER.toString());
        entity.setDomainId(domainId);
        entity.addPrimaryFilter("user", ugi.getShortUserName());
        TimelineEvent event = new TimelineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setEventType(DSEvent.DS_CONTAINER_START.toString());
        event.addEventInfo("Node", container.getNodeId().toString());
        event.addEventInfo("Resources", container.getResource().toString());
        entity.addEvent(event);

        try {
            ugi.doAs(new PrivilegedExceptionAction<TimelinePutResponse>() {
                @Override
                public TimelinePutResponse run() throws Exception {
                    return timelineClient.putEntities(entity);
                }
            });
        } catch (Exception e) {
            logger.error("Container start event could not be published for "
                            + container.getId().toString(),
                    e instanceof UndeclaredThrowableException ? e.getCause() : e);
        }
    }

    public void publishContainerEndEvent(
            final TimelineClient timelineClient, ContainerStatus container,
            String domainId, UserGroupInformation ugi) {
        final TimelineEntity entity = new TimelineEntity();
        entity.setEntityId(container.getContainerId().toString());
        entity.setEntityType(DSEntity.DS_CONTAINER.toString());
        entity.setDomainId(domainId);
        entity.addPrimaryFilter("user", ugi.getShortUserName());
        TimelineEvent event = new TimelineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setEventType(DSEvent.DS_CONTAINER_END.toString());
        event.addEventInfo("State", container.getState().name());
        event.addEventInfo("Exit Status", container.getExitStatus());
        entity.addEvent(event);

        try {
            ugi.doAs(new PrivilegedExceptionAction<TimelinePutResponse>() {
                @Override
                public TimelinePutResponse run() throws Exception {
                    return timelineClient.putEntities(entity);
                }
            });
        } catch (Exception e) {
            logger.error("Container end event could not be published for "
                            + container.getContainerId().toString(),
                    e instanceof UndeclaredThrowableException ? e.getCause() : e);
        }
    }

    private void publishApplicationAttemptEvent(
            final TimelineClient timelineClient, String appAttemptId,
            DSEvent appEvent, String domainId, UserGroupInformation ugi) {
        final TimelineEntity entity = new TimelineEntity();
        entity.setEntityId(appAttemptId);
        entity.setEntityType(DSEntity.DS_APP_ATTEMPT.toString());
        entity.setDomainId(domainId);
        entity.addPrimaryFilter("user", ugi.getShortUserName());
        TimelineEvent event = new TimelineEvent();
        event.setEventType(appEvent.toString());
        event.setTimestamp(System.currentTimeMillis());
        entity.addEvent(event);

        try {
            ugi.doAs(new PrivilegedExceptionAction<TimelinePutResponse>() {
                @Override
                public TimelinePutResponse run() throws Exception {
                    return timelineClient.putEntities(entity);
                }
            });
        } catch (Exception e) {
            logger.error("App Attempt "
                            + (appEvent.equals(DSEvent.DS_APP_ATTEMPT_START) ? "start" : "end")
                            + " event could not be published for "
                            + appAttemptId.toString(),
                    e instanceof UndeclaredThrowableException ? e.getCause() : e);
        }
    }


}
