package com.quakoo.framework.ext.push.context.handle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;

import com.quakoo.framework.ext.push.AbstractPushInfo;

/**
 * 推送基类上下文
 * class_name: PushBaseContextHandle
 * package: com.quakoo.framework.ext.push.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:03
 **/
public abstract class PushBaseContextHandle implements InitializingBean {

	 @Resource
	 protected AbstractPushInfo pushInfo;
	 
	 protected ExecutorService executorService = Executors.newCachedThreadPool();
	
}
