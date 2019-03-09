package com.quakoo.framework.ext.chat.context;

import com.quakoo.framework.ext.chat.context.handle.*;
//import com.quakoo.framework.ext.chat.context.handle.PushSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.distributed.DistributedSchedulerContextHandle;
import com.quakoo.framework.ext.chat.context.handle.nio.NioLongConnectionContextHandle;

public class ApiChatContextHandle {

	public static class LongConnectionContext extends LongConnectionContextHandle {}

	public static class ManyChatSchedulerContext extends ManyChatSchedulerContextHandle {}
	
	public static class NoticeAllSchedulerContext extends NoticeAllSchedulerContextHandle {} //通知的
	
	public static class NoticeRangeSchedulerContext extends NoticeRangeSchedulerContextHandle {} //通知的
	
//	public static class PushSchedulerContext extends PushSchedulerContextHandle {}
	
	public static class SingleChatSchedulerContext extends SingleChatSchedulerContextHandle {}
	
	public static class WillPushSchedulerContext extends WillPushSchedulerContextHandle {}
	
	
	
	public static class NioLongConnectionContext extends NioLongConnectionContextHandle {}
	
	public static class DistributedSchedulerContext extends DistributedSchedulerContextHandle {}


    public static class UserStreamSchedulerContext extends UserStreamSchedulerContextHandle {}

    public static class UserInfoSchedulerContext extends UserInfoSchedulerContextHandle {}
	
}
