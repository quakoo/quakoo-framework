package com.quakoo.framework.ext.chat.service.ext;

public interface ChatPushService {

	public int sendPush(long uid, String message) throws Exception;
	
}
