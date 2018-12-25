package com.quakoo.framework.ext.chat.distributed;

import java.util.List;

import com.google.common.collect.Lists;

public class DistributedConfig {

	public static List<String> canRunManyChatTable = Lists.newArrayList();
	
	public static List<String> canRunSingleChatTable = Lists.newArrayList();
	
    public static boolean canRunNoticeAll = false;
    
    public static boolean canRunNoticeRange = false;
    
    public static boolean canRunPush = false;
    
    public static boolean canRunWillPush = false;

}
