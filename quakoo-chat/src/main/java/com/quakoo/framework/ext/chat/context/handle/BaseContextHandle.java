package com.quakoo.framework.ext.chat.context.handle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.AbstractChatInfo;
import org.springframework.beans.factory.InitializingBean;


public abstract class BaseContextHandle implements InitializingBean {

	 @Resource
	 protected AbstractChatInfo chatInfo;
	 
	 protected ExecutorService executorService = Executors.newCachedThreadPool();
	
}
