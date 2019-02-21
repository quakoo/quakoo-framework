package com.quakoo.framework.ext.push.service;

import java.util.List;
import java.util.Map;

import com.quakoo.framework.ext.push.model.PushUserInfoPool;

public interface PushUserService {

	public boolean registUserInfo(long uid, int platform, int brand,
			String sessionId, String iosToken, String huaWeiToken, String meiZuPushId) throws Exception;

//	public boolean logoutUserInfo(long uid, int platform, int brand, String sessionId) throws Exception;

    public boolean logoutUserInfo(long uid) throws Exception;
	
	public List<PushUserInfoPool> getUserInfos(long uid) throws Exception;
	
	public Map<Long, List<PushUserInfoPool>> getUserInfos(List<Long> uids) throws Exception;
	
}
