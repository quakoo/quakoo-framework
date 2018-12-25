package com.quakoo.framework.ext.push.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.dao.PushUserInfoPoolDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushUserService;

public class PushUserServiceImpl extends BaseService implements PushUserService {

	@Resource
	private PushUserInfoPoolDao pushUserInfoPoolDao;
	
	@Resource
	private PushUserQueueDao pushUserQueueDao;
	
	@Override
	public boolean registUserInfo(long uid, int platform, int brand,
			String sessionId, String iosToken, String huaWeiToken, String meiZuPushId) throws Exception {
        pushUserInfoPoolDao.clear(uid);
		boolean res = false;
		PushUserInfoPool pool = new PushUserInfoPool();
		pool.setUid(uid);
		pool.setPlatform(platform);
		pool.setBrand(brand);
		pool.setSessionId(sessionId);
		pool.setIosToken(iosToken);
        pool.setHuaWeiToken(huaWeiToken);
        pool.setMeiZuPushId(meiZuPushId);
		res = pushUserInfoPoolDao.insert(pool);
	    PushUserQueue pushUserQueue = new PushUserQueue();
	    pushUserQueue.setUid(uid);
	    pushUserQueueDao.insert(pushUserQueue);
	    return res;
	}

    @Override
    public boolean logoutUserInfo(long uid) throws Exception {
        boolean res = pushUserInfoPoolDao.clear(uid);
        return res;
    }

    @Override
	public List<PushUserInfoPool> getUserInfos(long uid) throws Exception {
		return pushUserInfoPoolDao.getPushUserInfos(uid);
	}

	@Override
	public Map<Long, List<PushUserInfoPool>> getUserInfos(List<Long> uids)
			throws Exception {
		return pushUserInfoPoolDao.getPushUserInfos(uids);
	}
	
}
