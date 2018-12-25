package com.quakoo.framework.ext.push.context;

import com.quakoo.framework.ext.push.context.handle.PushDistributedSchedulerContextHandle;
import com.quakoo.framework.ext.push.context.handle.PushNioLongConnectionContextHandle;
import com.quakoo.framework.ext.push.context.handle.PushHandleAllSchedulerContextHandle;
import com.quakoo.framework.ext.push.context.handle.PushHandleSchedulerContextHandle;


public class ApiPushContextHandle {
	
	public static class PushHandleSchedulerContext extends PushHandleSchedulerContextHandle {}
	
	public static class PushHandleAllSchedulerContext extends PushHandleAllSchedulerContextHandle {}
	
	public static class PushNioLongConnectionContext extends PushNioLongConnectionContextHandle {}
	
	public static class PushDistributedSchedulerContext extends PushDistributedSchedulerContextHandle {}
	
}
