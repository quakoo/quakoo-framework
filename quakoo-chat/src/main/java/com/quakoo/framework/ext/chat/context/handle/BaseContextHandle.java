package com.quakoo.framework.ext.chat.context.handle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;

import com.quakoo.framework.ext.chat.AbstractChatInfo;

/**
 * 基类上下文
 * class_name: BaseContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:37
 **/
public abstract class BaseContextHandle implements InitializingBean {

	 @Resource
	 protected AbstractChatInfo chatInfo;
	 
	 protected ExecutorService executorService = Executors.newCachedThreadPool();
	
}
