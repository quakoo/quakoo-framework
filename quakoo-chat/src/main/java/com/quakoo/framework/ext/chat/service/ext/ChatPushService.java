package com.quakoo.framework.ext.chat.service.ext;

/**
 * 消息推送
 * class_name: ChatPushService
 * package: com.quakoo.framework.ext.chat.service.ext
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:10
 **/
public interface ChatPushService {

	public int sendPush(long uid, String message) throws Exception;
	
}
