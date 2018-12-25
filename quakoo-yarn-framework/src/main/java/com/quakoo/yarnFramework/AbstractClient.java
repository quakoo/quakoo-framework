package com.quakoo.yarnFramework;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by 136249 on 2015/3/10.
 */
public abstract class AbstractClient {
    Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    /**
     * 配置
     */
    public Configuration conf;

    public YarnClient yarnClient;
    /**
     * 是否让容器保持开启状态（可重复使用，避免重复开启），默认开启。
     * 任务如果比较特殊，执行频率较少，有不同的内存需求建议关闭
     */
    public boolean keepContainers = true;
    /**
     * 默认不重试
     */
    public int attemptFailuresValidityInterval = -1;

    /**
     * 默认不重试
     */
    public int maxAttempts=0;
    /**
     * application master 内存 默认64m
     */
    public int amMemory = 64;
    /**
     * application master 核数,默认1
     */
    public int amVCores = 1;
    /**
     * application master priority 资源优先级 默认为0
     */
    public int amPriority = 0;


    /**
     * 任务执行超时时间 默认5小时
     */
    public long clientTimeout = 1000 * 60 * 60 * 5;

    private long clientStartTime = 0;

    /**
     * am项目的路径
     * "/opt/project/syswin/scloud/SCloudTaskOnYarn/target/SCloudTaskOnYarn-jar-with-dependencies.jar"
     */
    public String getAmClassPath() {
        return "/data/app/taskOnYarn/SCloudYarnFramework-jar-with-dependencies.jar";
    }

    /**
     * application master 启动类
     */
    public String getAMMainClass() {
        return DefaultScloudAm.class.getCanonicalName();
    }

    /**
     * clientRequest
     *
     * @return ClientTaskRequest
     */
    public abstract ClientTaskRequest getClientTaskRequest();


    /**
     * 打印日志的地方
     *
     * @return stdoutLog
     */
    public String getStdoutLog(String applicationId) {
        return "> /data/app/taskOnYarn/amStdout_" + applicationId + ".log";
    }

    /**
     * 任务名字
     */
    public String getAppName() {
        return this.getClass().getSimpleName();
    }


    public void init(Configuration conf, YarnClient yarnClient) throws IOException, YarnException {
        if (conf == null) {
            conf = new YarnConfiguration();
        }
        if (yarnClient == null) {
            yarnClient = YarnClient.createYarnClient();
            yarnClient.init(conf);
            yarnClient.start();
            logger.info("start yarn client");
        }
        this.conf=conf;
        this.yarnClient=yarnClient;
        logClusterStatus(this.yarnClient);
    }

    public YarnClient getYarnClient() {
        return yarnClient;
    }

    public void setYarnClient(YarnClient yarnClient) {
        this.yarnClient = yarnClient;
    }

    public Configuration getConf() {
        return conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    public boolean submitApplication() throws IOException, YarnException {
        clientStartTime = System.currentTimeMillis();
        // Get a new application id
        YarnClientApplication app = yarnClient.createApplication();
        verifyAmResources(app);

        // set the application name
        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId appId = appContext.getApplicationId();
        appContext.setKeepContainersAcrossApplicationAttempts(keepContainers);
        appContext.setApplicationName(getAppName());
        if (attemptFailuresValidityInterval >= 0) {
            appContext.setAttemptFailuresValidityInterval(attemptFailuresValidityInterval);
        }
        if(maxAttempts>0) {
            appContext.setMaxAppAttempts(maxAttempts);
        }
        appContext.setResource(Resource.newInstance(amMemory, amVCores));
        appContext.setAMContainerSpec(getAmContainer(appId.toString()));
        appContext.setPriority(Priority.newInstance(amPriority));
        appContext.setQueue(getClientTaskRequest().getQueue());
        yarnClient.submitApplication(appContext);

        return monitorApplication(appId);
    }


    public ContainerLaunchContext getAmContainer(String applicationId) throws IOException, YarnException {
        ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
                null, getEnvironment(), getCommands(applicationId), null, null, null);
        if (UserGroupInformation.isSecurityEnabled()) {
            Credentials credentials = new Credentials();
            String tokenRenewer = conf.get(YarnConfiguration.RM_PRINCIPAL);
            if (tokenRenewer == null || tokenRenewer.length() == 0) {
                throw new IOException("Can't get Master Kerberos principal for the RM to use as renewer");
            }
            FileSystem fs = FileSystem.get(conf);
            final org.apache.hadoop.security.token.Token<?> tokens[] =
                    fs.addDelegationTokens(tokenRenewer, credentials);
            if (tokens != null) {
                for (org.apache.hadoop.security.token.Token<?> token : tokens) {
                    logger.info("Got dt for " + fs.getUri() + "; " + token);
                }
            }
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            ByteBuffer fsTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
            amContainer.setTokens(fsTokens);
        }
        return amContainer;
    }

    public List<String> getCommands(String applicationId) throws IOException, YarnException {
        Vector<CharSequence> vargs = new Vector<CharSequence>(30);
        vargs.add("java");
        vargs.add("-Xmx" + amMemory + "m");
        vargs.add(getAMMainClass());
        ClientTaskRequest clientTaskRequest = getClientTaskRequest();
        if (StringUtils.isBlank(clientTaskRequest.getTaskCmd()) ) {
            throw new YarnException("clientTaskRequest is error :" + clientTaskRequest);
        }
        vargs.add("--" + TaskOnYarnConstans.CLINETTASKREUQESTPARAM + " "
                + URLEncoder.encode(Base64Util.encode(JsonUtils.format(clientTaskRequest).getBytes()),"utf-8"));
        vargs.add(getStdoutLog(applicationId));
        // Get final commmand
        StringBuilder command = new StringBuilder();
        for (CharSequence str : vargs) {
            command.append(str).append(" ");
        }
        logger.info("Completed setting up app master command " + command.toString());
        List<String> commands = new ArrayList<String>();
        commands.add(command.toString());
        return commands;
    }

    public Map<String, String> getEnvironment() {
        Map<String, String> env = new HashMap<String, String>();
        StringBuilder classPathEnv = new StringBuilder(ApplicationConstants.Environment.CLASSPATH.$$())
                .append(ApplicationConstants.CLASS_PATH_SEPARATOR).append("./*");
        for (String c : conf.getStrings(
                YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                YarnConfiguration.DEFAULT_YARN_CROSS_PLATFORM_APPLICATION_CLASSPATH)) {
            classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR);
            classPathEnv.append(c.trim());
        }
        classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR).append("./log4j.properties");

        // add the runtime classpath needed for tests to work
        if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
            classPathEnv.append(':');
            classPathEnv.append(System.getProperty("java.class.path"));
        }
        classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR);
        classPathEnv.append(getAmClassPath());
        env.put("CLASSPATH", classPathEnv.toString());
        return env;
    }

    /**
     * 记录集群状态
     *
     * @param yarnClient yarnClient
     * @throws IOException
     * @throws YarnException
     */
    public void logClusterStatus(YarnClient yarnClient) throws IOException, YarnException {
        YarnClusterMetrics clusterMetrics = yarnClient.getYarnClusterMetrics();
        logger.info("Got Cluster metric info from ASM"
                + ", numNodeManagers=" + clusterMetrics.getNumNodeManagers());

        List<NodeReport> clusterNodeReports = yarnClient.getNodeReports(
                NodeState.RUNNING);
        logger.info("Got Cluster node info from ASM");
        for (NodeReport node : clusterNodeReports) {
            logger.info("Got node report from ASM for"
                    + ", nodeId=" + node.getNodeId()
                    + ", nodeAddress" + node.getHttpAddress()
                    + ", nodeRackName" + node.getRackName()
                    + ", nodeNumContainers" + node.getNumContainers());
        }

        QueueInfo queueInfo = yarnClient.getQueueInfo(getClientTaskRequest().getQueue());
        if (queueInfo != null) {
            logger.info("Queue info"
                    + ", queueName=" + queueInfo.getQueueName()
                    + ", queueCurrentCapacity=" + queueInfo.getCurrentCapacity()
                    + ", queueMaxCapacity=" + queueInfo.getMaximumCapacity()
                    + ", queueApplicationCount=" + queueInfo.getApplications().size()
                    + ", queueChildQueueCount=" + queueInfo.getChildQueues().size());
        } else {
            throw new YarnException("no queue:" + getClientTaskRequest().getQueue());
        }

        List<QueueUserACLInfo> listAclInfo = yarnClient.getQueueAclsInfo();
        for (QueueUserACLInfo aclInfo : listAclInfo) {
            for (QueueACL userAcl : aclInfo.getUserAcls()) {
                logger.info("User ACL Info for Queue"
                        + ", queueName=" + aclInfo.getQueueName()
                        + ", userAcl=" + userAcl.name());
            }
        }
    }

    /**
     * 修正申请am的资源
     *
     * @param app
     */
    private void verifyAmResources(YarnClientApplication app) {
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
        int maxMem = appResponse.getMaximumResourceCapability().getMemory();
        int maxVCores = appResponse.getMaximumResourceCapability().getVirtualCores();

        if (amMemory > maxMem) {
            logger.info("AM memory specified above max threshold of cluster. Using max value."
                    + ", specified=" + amMemory
                    + ", max=" + maxMem);
            amMemory = maxMem;
        }
        if (amVCores > maxVCores) {
            logger.info("AM virtual cores specified above max threshold of cluster. "
                    + "Using max value." + ", specified=" + amVCores
                    + ", max=" + maxVCores);
            amVCores = maxVCores;
        }
    }


    public boolean monitorApplication(ApplicationId appId)
            throws YarnException, IOException {

        while (true) {

            // Check app status every 1 second.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.debug("Thread sleep in monitoring loop interrupted");
            }

            // Get application report for the appId we are interested in
            ApplicationReport report = yarnClient.getApplicationReport(appId);

            logger.info("Got application report from ASM for"
                    + ", appId=" + appId.getId()
                    + ", clientToAMToken=" + report.getClientToAMToken()
                    + ", appDiagnostics=" + report.getDiagnostics()
                    + ", appMasterHost=" + report.getHost()
                    + ", appQueue=" + report.getQueue()
                    + ", appMasterRpcPort=" + report.getRpcPort()
                    + ", appStartTime=" + report.getStartTime()
                    + ", yarnAppState=" + report.getYarnApplicationState().toString()
                    + ", distributedFinalState=" + report.getFinalApplicationStatus().toString()
                    + ", appTrackingUrl=" + report.getTrackingUrl()
                    + ", appUser=" + report.getUser());

            YarnApplicationState state = report.getYarnApplicationState();
            FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
            if (YarnApplicationState.FINISHED == state) {
                if (FinalApplicationStatus.SUCCEEDED == dsStatus) {
                    logger.info("Application has completed successfully. Breaking monitoring loop");
                    return true;
                } else {
                    logger.info("Application did finished unsuccessfully."
                            + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                            + ". Breaking monitoring loop");
                    return false;
                }
            } else if (YarnApplicationState.KILLED == state
                    || YarnApplicationState.FAILED == state) {
                logger.info("Application did not finish."
                        + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                        + ". Breaking monitoring loop");
                return false;
            }

            if (System.currentTimeMillis() > (clientStartTime + clientTimeout)) {
                logger.info("Reached client specified timeout for application. Killing application");
                forceKillApplication(appId);
                return false;
            }
        }

    }


    public void forceKillApplication(ApplicationId appId)
            throws YarnException, IOException {
        yarnClient.killApplication(appId);
    }

}
