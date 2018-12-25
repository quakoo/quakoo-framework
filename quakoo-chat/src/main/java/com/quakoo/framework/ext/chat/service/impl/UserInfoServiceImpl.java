package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.dao.PushQueueDao;
import com.quakoo.framework.ext.chat.dao.UserInfoDao;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.service.UserInfoService;


public class UserInfoServiceImpl implements UserInfoService {

	@Resource
	private UserInfoDao userInfoDao;

	@Resource
	private PushQueueDao pushQueueDao;

//	private ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

	public UserInfo syncUserInfo(final long uid, double lastIndex, UserInfo userInfo) throws Exception {
//        try {
//            threadPool.execute(new Runnable() {
//                @Override
//                public void run() {
//                    pushQueueDao.clear(uid);
//                }
//            });
//        } catch (Exception e) {}
		double loginTime = userInfoDao.create_login_time(uid);
        userInfo = userInfoDao.sync(uid, lastIndex, loginTime, userInfo);
//		if(null == userInfo) {
//			userInfo = new UserInfo();
//			userInfo.setLastIndex(lastIndex);
//			userInfo.setLoginTime(loginTime);
//			userInfo.setPromptIndex(0);
//			userInfo.setUid(uid);
//			userInfo = userInfoDao.insert(userInfo);
//		} else {
//            userInfo.setLastIndex(lastIndex);
//            userInfo.setLoginTime(loginTime);
//			userInfoDao.update(userInfo);
//		}
		return userInfo;
	}

	public boolean updatePromptIndex(long uid, double promptIndex)
			throws Exception {
		return userInfoDao.update_prompt_index(uid, promptIndex);
	}
	
	public List<UserInfo> list(String tableName, double loginTime, int size)
			throws Exception {
		return userInfoDao.list(tableName, loginTime, size);
	}

	@Override
	public List<UserInfo> load(List<Long> uids) throws Exception {
		return userInfoDao.loads(uids);
	}

    @Override
    public UserInfo load(long uid) throws Exception {
        return userInfoDao.load(uid);
    }

}
