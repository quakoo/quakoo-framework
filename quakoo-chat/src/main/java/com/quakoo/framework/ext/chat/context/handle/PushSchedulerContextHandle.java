//package com.quakoo.framework.ext.chat.context.handle;
//
//import java.util.concurrent.TimeUnit;
//
//import javax.annotation.Resource;
//
//import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.common.util.concurrent.Uninterruptibles;
//import com.quakoo.framework.ext.chat.service.PushQueueService;
//
//public class PushSchedulerContextHandle extends BaseContextHandle {
//
//    Logger logger = LoggerFactory.getLogger(PushSchedulerContextHandle.class);
//
//	private int handle_size = 500;
//
//	@Resource
//	private PushQueueService pushQueueService;
//
//	@Override
//	public void afterPropertiesSet() throws Exception {
//		Thread thread = new Thread(new Processer());
//		thread.start();
//	}
//
//	class Processer implements Runnable {
//		@Override
//		public void run() {
//			while(true) {
//				if(DistributedConfig.canRunPush) {
//					Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
//					try {
//						pushQueueService.handle(handle_size);
//					} catch (Exception e) {
//						logger.error(e.getMessage(), e);
//					}
//				} else {
//					Uninterruptibles.sleepUninterruptibly(15, TimeUnit.SECONDS);
//				}
//			}
//		}
//	}
//
//}
