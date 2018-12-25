package com.quakoo.yarnFramework;

import com.quakoo.baseFramework.json.JsonUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by 136249 on 2015/3/11.
 */
public class ClientTaskRequest {

  private String taskCmd;

    /**
     * 任务优先级
     */
    private int taskPriority = 0;

    /**
     * 容器内存，默认1000
     */
    public int containerMemory = 1000;
    /**
     * 容器核数，默认1个
     */
    public int containerVirtualCores = 1;
    /**
     * 使用容器数量,默认1个
     */
    public int numContainers = 1;

    /**
     * 队列名称，默认为default
     */
    public String queue = "default";

    /**
     * 传递给任务的参数
     */
    private LinkedBlockingQueue<Map<String, String>> taskParams;


    public LinkedBlockingQueue<Map<String, String>> getTaskParams() {
        return taskParams;
    }

    public void setTaskParams(LinkedBlockingQueue<Map<String, String>> taskParams) {
        this.taskParams = taskParams;
    }



    public int getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(int taskPriority) {
        this.taskPriority = taskPriority;
    }

    public int getContainerMemory() {
        return containerMemory;
    }

    public void setContainerMemory(int containerMemory) {
        this.containerMemory = containerMemory;
    }

    public int getContainerVirtualCores() {
        return containerVirtualCores;
    }

    public void setContainerVirtualCores(int containerVirtualCores) {
        this.containerVirtualCores = containerVirtualCores;
    }

    public int getNumContainers() {
        return numContainers;
    }

    public void setNumContainers(int numContainers) {
        this.numContainers = numContainers;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getTaskCmd() {
        return taskCmd;
    }

    public void setTaskCmd(String taskCmd) {
        this.taskCmd = taskCmd;
    }

    @Override
    public String toString() {
        return "ClientTaskRequest{" +
                "taskCmd='" + taskCmd + '\'' +
                ", taskPriority=" + taskPriority +
                ", containerMemory=" + containerMemory +
                ", containerVirtualCores=" + containerVirtualCores +
                ", numContainers=" + numContainers +
                ", queue='" + queue + '\'' +
                ", taskParams=" + taskParams +
                '}';
    }

    public static void main(String[] few) throws IOException {
        System.out.println(JsonUtils.format(new ClientTaskRequest()));
    }
}
