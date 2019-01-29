package com.quakoo.framework.ext.chat.context.handle;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.service.WillPushQueueService;

/**
 * 离线消息推送上下文
 * class_name: WillPushSchedulerContextHandle
 * package: com.quakoo.framework.ext.chat.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:46
 **/
public class WillPushSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(WillPushSchedulerContextHandle.class);
	
	@Resource
	private WillPushQueueService willPushQueueService;

	@Override
	public void afterPropertiesSet() throws Exception {
		Thread thread = new Thread(new Processer());
		thread.start();
	}
	
	class Processer implements Runnable {

		@Override
		public void run() {
			while(true) {
				if(DistributedConfig.canRunWillPush) {
                    SleepUtils.sleep(200, 50);
                    long time = System.currentTimeMillis();
					try {
						willPushQueueService.handle(time); //操作类
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				} else {
					Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
				}
			}
		}
	}
	
}
