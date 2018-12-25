package com.quakoo.framework.ext.chat.context;

import com.quakoo.framework.ext.chat.context.handle.LongConnectionContextHandle;
import com.quakoo.framework.ext.chat.context.handle.ManyChatSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.NoticeAllSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.NoticeRangeSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.PushSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.SingleChatSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.WillPushSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.distributed.DistributedSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.nio.NioLongConnectionContextHandle;

public class ApiChatContextHandle {

	public static class LongConnectionContext extends LongConnectionContextHandle {}

	public static class ManyChatSchedulerContext extends ManyChatSchedulerContextHandle {}
	
	public static class NoticeAllSchedulerContext extends NoticeAllSchedulerContextHandle {} //通知的
	
	public static class NoticeRangeSchedulerContext extends NoticeRangeSchedulerContextHandle {} //通知的
	
	public static class PushSchedulerContext extends PushSchedulerContextHandle {}
	
	public static class SingleChatSchedulerContext extends SingleChatSchedulerContextHandle {}
	
	public static class WillPushSchedulerContext extends WillPushSchedulerContextHandle {}
	
	
	
	public static class NioLongConnectionContext extends NioLongConnectionContextHandle {}
	
	public static class DistributedSchedulerContext extends DistributedSchedulerContextHandle {}
	
}
