package com.quakoo.framework.ext.push.context.handle;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.model.constant.Brand;
import com.quakoo.framework.ext.push.model.constant.Platform;
import com.quakoo.framework.ext.push.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class PushBasePushHandleContextHandle extends PushBaseContextHandle {

    Logger logger = LoggerFactory.getLogger(PushBasePushHandleContextHandle.class);

    @Resource
    private InternalPushService internalPushService;

    @Resource
    private PushUserService pushUserService;

    @Resource
    private IosPushService iosPushService;

    @Resource
    private AndroidXiaoMiPushService androidXiaoMiPushService;

    @Resource
    private AndroidHuaWeiPushService androidHuaWeiPushService;

    @Resource
    private AndroidMeiZuPushService androidMeiZuPushService;

    protected void handleSingle(long uid, PushMsg pushMsg) {
        try {
            List<PushUserInfoPool> pools = pushUserService.getUserInfos(uid);
            List<PushUserInfoPool> iosUserInfos = Lists.newArrayList();
            List<PushUserInfoPool> androidXiaoMiUserInfos = Lists.newArrayList();
            List<PushUserInfoPool> androidHuaWeiUserInfos = Lists.newArrayList();
            List<PushUserInfoPool> androidMeiZuUserInfos = Lists.newArrayList();
            long internalUid = 0;
            int pushPlatform = pushMsg.getPlatform();
            for(PushUserInfoPool pool : pools) {
                int platform = pool.getPlatform();
                if(pushPlatform == Platform.all || pushPlatform == platform) {
                    int brand = pool.getBrand();
                    String iosToken = pool.getIosToken();
                    String huaWeiToken = pool.getHuaWeiToken();
                    String meiZuPushId = pool.getMeiZuPushId();
                    if(platform == Platform.ios && StringUtils.isNotBlank(iosToken)) {
                        iosUserInfos.add(pool);
                    } else if (platform == Platform.android) {
                        if(brand == Brand.xiaomi) {
                            androidXiaoMiUserInfos.add(pool);
                        } else if(brand == Brand.huawei && StringUtils.isNotBlank(huaWeiToken)) {
                            androidHuaWeiUserInfos.add(pool);
                        } else if(brand == Brand.meizu && StringUtils.isNotBlank(meiZuPushId)) {
                            androidMeiZuUserInfos.add(pool);
                        } else {
                            internalUid = pool.getUid();
                        }

                    }
                }
            }
            if(internalUid > 0) internalPushService.push(uid, pushMsg);
            if(iosUserInfos.size() > 0) {
                iosPushService.batchPush(iosUserInfos, pushMsg);
            }
            if(androidXiaoMiUserInfos.size() > 0) {
                androidXiaoMiPushService.batchPush(androidXiaoMiUserInfos, pushMsg);
            }
            if(androidHuaWeiUserInfos.size() > 0) {
                androidHuaWeiPushService.batchPush(androidHuaWeiUserInfos, pushMsg);
            }
            if(androidMeiZuUserInfos.size() > 0) {
                androidMeiZuPushService.batchPush(androidMeiZuUserInfos, pushMsg);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    protected void handleBatch(List<Long> uids, PushMsg pushMsg) {
        try {
            Map<Long, List<PushUserInfoPool>> poolMap = pushUserService.getUserInfos(uids);
            List<PushUserInfoPool> iosUserInfos = Lists.newArrayList();
            List<PushUserInfoPool> androidXiaoMiUserInfos = Lists.newArrayList();
            List<PushUserInfoPool> androidHuaWeiUserInfos = Lists.newArrayList();
            List<PushUserInfoPool> androidMeiZuUserInfos = Lists.newArrayList();
            Set<Long> internalUids = Sets.newHashSet();
            int pushPlatform = pushMsg.getPlatform();
            for(Entry<Long, List<PushUserInfoPool>> entry : poolMap.entrySet()) {
                long uid = entry.getKey().longValue();
                List<PushUserInfoPool> pools = entry.getValue();
                for(PushUserInfoPool pool : pools) {
                    int platform = pool.getPlatform();
                    if(pushPlatform == Platform.all || pushPlatform == platform) {
                        int brand = pool.getBrand();
                        String iosToken = pool.getIosToken();
                        String huaWeiToken = pool.getHuaWeiToken();
                        String meiZuPushId = pool.getMeiZuPushId();
                        if(platform == Platform.ios && StringUtils.isNotBlank(iosToken)) {
                            iosUserInfos.add(pool);
                        } else if (platform == Platform.android) {
                            if(brand == Brand.xiaomi) {
                                androidXiaoMiUserInfos.add(pool);
                            } else if (brand == Brand.huawei && StringUtils.isNotBlank(huaWeiToken)) {
                                androidHuaWeiUserInfos.add(pool);
                            } else if(brand == Brand.meizu && StringUtils.isNotBlank(meiZuPushId)) {
                                androidMeiZuUserInfos.add(pool);
                            } else {
                                internalUids.add(uid);
                            }
                        }
                    }
                }
            }
            if(iosUserInfos.size() > 0) {
                iosPushService.batchPush(iosUserInfos, pushMsg);
            }
            if(internalUids.size() > 0) {
                internalPushService.batchPush(Lists.newArrayList(internalUids), pushMsg);
            }
            if(androidXiaoMiUserInfos.size() > 0) {
                androidXiaoMiPushService.batchPush(androidXiaoMiUserInfos, pushMsg);
            }
            if(androidHuaWeiUserInfos.size() > 0) {
                androidHuaWeiPushService.batchPush(androidHuaWeiUserInfos, pushMsg);
            }
            if(androidMeiZuUserInfos.size() > 0) {
                androidMeiZuPushService.batchPush(androidMeiZuUserInfos, pushMsg);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
	
}
