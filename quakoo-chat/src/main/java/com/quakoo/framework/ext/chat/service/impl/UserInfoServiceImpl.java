package com.quakoo.framework.ext.chat.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.dao.UserInfoDao;
import com.quakoo.framework.ext.chat.dao.UserInfoQueueDao;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.model.UserInfoQueue;
import com.quakoo.framework.ext.chat.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 用户信息处理类
 * class_name: UserInfoServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:22
 **/
public class UserInfoServiceImpl implements UserInfoService, InitializingBean {

    private final static int handle_num = 20;

    private final static int handle_expire_time = 1000 * 60 * 5;

    Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

	@Resource
	private UserInfoDao userInfoDao;

	@Resource
	private UserInfoQueueDao userInfoQueueDao;

    private static volatile LinkedBlockingQueue<UserInfo> persistent_queue = new LinkedBlockingQueue<UserInfo>(); //用户信息队列

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    /**
     * 异步持久化用户信息
     * class_name: UserInfoServiceImpl
     * package: com.quakoo.framework.ext.chat.service.impl
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 18:23
     **/
    class Processer implements Runnable {
        @Override
        public void run() {
            Map<Long, UserInfo> batchMap = Maps.newHashMap();
            while (true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    UserInfo userInfo = persistent_queue.poll(1, TimeUnit.SECONDS);
                    if (null != userInfo) {
                        batchMap.put(userInfo.getUid(), userInfo);
                        if (batchMap.size() >= handle_num) {
//                            List<UserInfo> userInfos = Lists.newArrayList(batchMap.values());
                            List<UserInfoQueue> queues = Lists.newArrayList();
                            for(UserInfo one : batchMap.values()) {
                                UserInfoQueue queue = new UserInfoQueue(one);
                                queues.add(queue);
                            }
                            userInfoQueueDao.insert(queues);
//                            userInfoDao.replace(userInfos); //批量替换用户信息
//                            userInfos.clear();
                            queues.clear();
                            batchMap.clear();
                        }
                    }
                    List<UserInfoQueue> queues = Lists.newArrayList();
                    for (Iterator<Long> it = batchMap.keySet().iterator(); it.hasNext(); ) {
                        Long key = it.next();
                        UserInfo value = batchMap.get(key);
                        long loginTime = (long) value.getLoginTime();
                        if (currentTime - loginTime > handle_expire_time) {
                            UserInfoQueue queue = new UserInfoQueue(value);
                            queues.add(queue);
                            it.remove();
                        }
                    }
                    if (queues.size() > 0) {
                        userInfoQueueDao.insert(queues);
//                        userInfoDao.replace(userInfos);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void replace(List<UserInfo> userInfos) throws Exception {
        userInfoDao.replace(userInfos);
    }

    public static void main(String[] args) {
        Map<Integer, String> map = Maps.newHashMap();
        map.put(1, "a");
        map.put(2, "b");
        for(Iterator<Integer> it = map.keySet().iterator(); it.hasNext();) {
            Integer key = it.next();
            String value = map.get(key);
            if(value.equals("a")) {
                it.remove();
            }
        }
        System.out.println(map.toString());
    }

    /**
     * 同步用户信息
     * method_name: syncUserInfo
     * params: [uid, lastIndex, userInfo]
     * return: com.quakoo.framework.ext.chat.model.UserInfo
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 18:23
     **/
    public UserInfo syncUserInfo(final long uid, double lastIndex, UserInfo userInfo) throws Exception {
        if(userInfo != null && userInfo.getLastIndex() == lastIndex) {
//            logger.info("==== hit lastIndex not change uid : " + uid + " ,lastIndex : " + lastIndex);
            return userInfo;
        }
        double loginTime = userInfoDao.create_login_time(uid);
        if(lastIndex > loginTime) lastIndex = loginTime;
        if(userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setLastIndex(lastIndex);
            userInfo.setLoginTime(loginTime);
            userInfo.setPromptIndex(0);
            userInfo.setUid(uid);
        } else {
            userInfo.setLastIndex(lastIndex);
            userInfo.setLoginTime(loginTime);
        }
        userInfoDao.cache_user_info(userInfo); //更新缓存
        persistent_queue.add(userInfo); //插入到异步队列
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

    @Override
    public List<UserInfo> loadCache(List<Long> uids) throws Exception {
        return userInfoDao.load_cache(uids);
    }
}
