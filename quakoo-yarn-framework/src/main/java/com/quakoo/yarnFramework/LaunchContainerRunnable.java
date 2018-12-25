package com.quakoo.yarnFramework;


import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by 136249 on 2015/3/10.
 */
public class LaunchContainerRunnable implements Runnable {
    Logger logger = LoggerFactory.getLogger(LaunchContainerRunnable.class);
    // Allocated container
    Container container;

    NMCallbackHandler containerListener;

    AbstractAm am;

    Map<String, String> taskParams;


    /**
     * @param container         Allocated container
     * @param containerListener Callback handler of the container
     */
    public LaunchContainerRunnable(
            Container container, NMCallbackHandler containerListener, AbstractAm am,Map<String, String> taskParams) {
        this.container = container;
        this.containerListener = containerListener;
        this.am = am;
        this.taskParams=taskParams;
    }

    @Override
    public void run() {
        logger.info("Setting up container launch container for containerid:{},taskRequest:{}", container.getId(), taskParams);
        ClientTaskRequest clientTaskRequest = am.getAmContext().getClientTaskRequest();
        String taskCmd=clientTaskRequest.getTaskCmd();
        String strTaskParams = "";
        if (taskParams != null) {
            try {
                strTaskParams = URLEncoder.encode(Base64Util.encode(JsonUtils.format(taskParams).getBytes()),"utf-8");
            } catch (Exception e) {
                throw new RuntimeException("format taskParams error:" + taskParams, e);
            }
        }
        String[] args = {taskCmd +" "+ strTaskParams};
        // Get final commmand
        StringBuilder command = new StringBuilder();
        for (CharSequence str : args) {
            command.append(str).append(" ");
        }
        List<String> commands = new ArrayList<String>();
        commands.add(command.toString());
        logger.info("command:{}", command.toString());
        ContainerLaunchContext ctx = ContainerLaunchContext.newInstance(
                null, null, commands, null, am.allTokens.duplicate(), null);
        containerListener.addContainer(container.getId(), container);
        //调用node管理器的客户端 启动容器
        am.nmClientAsync.startContainerAsync(container, ctx);
    }
}