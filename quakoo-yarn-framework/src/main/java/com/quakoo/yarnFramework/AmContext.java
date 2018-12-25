package com.quakoo.yarnFramework;

import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.client.api.TimelineClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 136249 on 2015/3/10.
 */
public class AmContext {

    private ClientTaskRequest clientTaskRequest;

    private ApplicationAttemptId appAttemptID;

    public TimelineClient timelineClient;

    private Map<ContainerId,Map<String,String>> taskContext=new ConcurrentHashMap<>();


    public ClientTaskRequest getClientTaskRequest() {
        return clientTaskRequest;
    }

    public void setClientTaskRequest(ClientTaskRequest clientTaskRequest) {
        this.clientTaskRequest = clientTaskRequest;
    }

    public ApplicationAttemptId getAppAttemptID() {
        return appAttemptID;
    }

    public void setAppAttemptID(ApplicationAttemptId appAttemptID) {
        this.appAttemptID = appAttemptID;
    }

    public TimelineClient getTimelineClient() {
        return timelineClient;
    }

    public void setTimelineClient(TimelineClient timelineClient) {
        this.timelineClient = timelineClient;
    }

    public Map<ContainerId, Map<String, String>> getTaskContext() {
        return taskContext;
    }

    public void setTaskContext(Map<ContainerId, Map<String, String>> taskContext) {
        this.taskContext = taskContext;
    }
}
