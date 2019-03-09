package com.quakoo.framework.ext.chat.distributed;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * 分布式配置信息
 * class_name: DistributedConfig
 * package: com.quakoo.framework.ext.chat.distributed
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:03
 **/
public class DistributedConfig {

//	public static List<String> canRunManyChatTable = Lists.newArrayList(); //本机能运行的群聊消息队列名称
	
//	public static List<String> canRunSingleChatTable = Lists.newArrayList(); //本机能运行的单聊消息队列名称

    public static List<String> canRunManyQueue = Lists.newArrayList(); //本机能运行的群聊消息队列名称

    public static List<String> canRunSingleQueue = Lists.newArrayList(); //本机能运行的单聊消息队列名称

    public static List<String> canRunUserStreamQueue = Lists.newArrayList(); //本机能运行的用户信息流队列名称

    public static List<String> canRunUserInfoQueue = Lists.newArrayList(); //本机能运行的用户信息队列名称
	
    public static boolean canRunNoticeAll = false;
    
    public static boolean canRunNoticeRange = false;
    
//    public static boolean canRunPush = false;

    public static boolean canRunClean = false;
    
    public static boolean canRunWillPush = false; //本机是否能运行推送

}
