package com.quakoo.framework.ext.push.context.handle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.AbstractPushInfo;
import org.springframework.beans.factory.InitializingBean;

public abstract class PushBaseContextHandle implements InitializingBean {

	 @Resource
	 protected AbstractPushInfo pushInfo;
	 
	 protected ExecutorService executorService = Executors.newCachedThreadPool();
	
}
