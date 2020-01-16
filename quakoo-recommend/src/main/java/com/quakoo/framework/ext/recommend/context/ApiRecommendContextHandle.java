package com.quakoo.framework.ext.recommend.context;

import com.quakoo.framework.ext.recommend.context.handle.*;

public class ApiRecommendContextHandle {

    public static class DistributedSchedulerContext extends DistributedSchedulerContextHandle {}

    public static class IDFMissWordSchedulerContext extends IDFMissWordSchedulerContextHandle {}

    public static class HotWordSchedulerContext extends HotWordSchedulerContextHandle {}

    public static class PortraitItemCFSchedulerContext extends PortraitItemCFSchedulerContextHandle {}

    public static class SyncInfoSchedulerContext extends SyncInfoSchedulerContextHandle {}

}
