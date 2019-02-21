package com.quakoo.framework.ext.push.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.bean.PushUserInfoMsg;
import com.quakoo.framework.ext.push.dao.PushUserInfoPoolDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.LocalCacheService;
import com.quakoo.framework.ext.push.service.PushUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 推送用户信息处理类
 * class_name: PushUserServiceImpl
 * package: com.quakoo.framework.ext.push.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 15:01
 **/
public class PushUserServiceImpl extends BaseService implements PushUserService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(PushUserServiceImpl.class);

    private final static int handle_num = 20; //批量处理条数

    private final static int handle_expire_time = 1000 * 60 * 5; //处理超时时间

    private String cache_user_info_key = "user_info_%d";

	@Resource
	private PushUserInfoPoolDao pushUserInfoPoolDao;
	
	@Resource
	private PushUserQueueDao pushUserQueueDao;

	@Resource
	private LocalCacheService localCacheService;

    private static volatile LinkedBlockingQueue<PushUserInfoMsg> queue = new LinkedBlockingQueue<PushUserInfoMsg>(); //待处理队列

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class Processer implements Runnable {

        private void handle(List<PushUserInfoMsg> list) {
            Map<Long, PushUserInfoPool> registMap = Maps.newHashMap();
            List<Long> logoutList = Lists.newArrayList();
            for(int i = list.size() - 1; i >= 0; i--) {
                PushUserInfoMsg one = list.get(i);
                if(!registMap.keySet().contains(one.getUid()) && !logoutList.contains(one.getUid())) {
                    int type = one.getType();
                    if(type == PushUserInfoMsg.type_regist) {
                        PushUserInfoPool pool = new PushUserInfoPool();
                        pool.setUid(one.getUid());
                        pool.setPlatform(one.getPlatform());
                        pool.setBrand(one.getBrand());
                        pool.setSessionId(one.getSessionId());
                        pool.setIosToken(one.getIosToken());
                        pool.setHuaWeiToken(one.getHuaWeiToken());
                        pool.setMeiZuPushId(one.getMeiZuPushId());
                        registMap.put(one.getUid(), pool);
                    } else {
                        logoutList.add(one.getUid());
                    }
                }
            }
            if(registMap.size() > 0) {
                pushUserInfoPoolDao.insert(Lists.newArrayList(registMap.values())); //持久化到推送用户信息表
                pushUserQueueDao.insert(Lists.newArrayList(registMap.keySet())); //持久化到推送用户队列表
            }
            if(logoutList.size() > 0) {
                pushUserInfoPoolDao.clear(logoutList); //清除推送用户信息
                pushUserQueueDao.delete(logoutList); //清除推送用户队列
            }
        }

        @Override
        public void run() {
            List<PushUserInfoMsg> batchList = Lists.newArrayList();
            while (true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    PushUserInfoMsg msg = queue.poll(1, TimeUnit.SECONDS);
                    if (null != msg) {
                        batchList.add(msg);
                        if (batchList.size() >= handle_num) {
                            handle(batchList);
                            batchList.clear();
                        }
                    }
                    List<PushUserInfoMsg> list = Lists.newArrayList();
                    for (Iterator<PushUserInfoMsg> it = batchList.iterator(); it.hasNext(); ) {
                        PushUserInfoMsg one = it.next();
                        if (currentTime - one.getTime() > handle_expire_time) {
                            list.add(one);
                            it.remove();
                        }
                    }
                    if (list.size() > 0) {
                        handle(list);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 注册用户信息
     * method_name: registUserInfo
     * params: [uid, platform, brand, sessionId, iosToken, huaWeiToken, meiZuPushId]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 15:04
     **/
    @Override
	public boolean registUserInfo(long uid, int platform, int brand,
			String sessionId, String iosToken, String huaWeiToken, String meiZuPushId) throws Exception {
        PushUserInfoPool pool = new PushUserInfoPool();
        pool.setUid(uid);
        pool.setPlatform(platform);
        pool.setBrand(brand);
        pool.setSessionId(sessionId);
        pool.setIosToken(iosToken);
        pool.setHuaWeiToken(huaWeiToken);
        pool.setMeiZuPushId(meiZuPushId);

        String key = String.format(cache_user_info_key, uid);

        String now = JsonUtils.toJson(pool);
        String old = localCacheService.getString(key);

        if(StringUtils.isNotBlank(old) && old.equals(now)) { //如果已经存储了最新的用户推送信息，则不再存储到redis和mysql里
            logger.info("==== hit local cache uid : " + uid);
            return true;
        } else {
            boolean res = pushUserInfoPoolDao.cache_insert(pool); //更新缓存
            if(res) {
                PushUserInfoMsg msg = new PushUserInfoMsg();
                msg.setType(PushUserInfoMsg.type_regist);
                msg.setUid(uid);
                msg.setPlatform(platform);
                msg.setBrand(brand);
                msg.setSessionId(sessionId);
                msg.setIosToken(iosToken);
                msg.setHuaWeiToken(huaWeiToken);
                msg.setMeiZuPushId(meiZuPushId);
                msg.setTime(System.currentTimeMillis());
                queue.add(msg); //添加到待处理队列

                localCacheService.set(key, now);
            }
            return res;
        }
	}

	/**
     * 登出用户信息
	 * method_name: logoutUserInfo
	 * params: [uid]
	 * return: boolean
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 15:13
	 **/
    @Override
    public boolean logoutUserInfo(long uid) throws Exception {
        String key = String.format(cache_user_info_key, uid);
        localCacheService.remove(key);
//        logger.info("====== logoutUserInfo : " + uid);
        boolean res = pushUserInfoPoolDao.cache_clear(uid); //缓存清除
        if(res) {
            PushUserInfoMsg msg = new PushUserInfoMsg();
            msg.setType(PushUserInfoMsg.type_logout);
            msg.setUid(uid);
            msg.setTime(System.currentTimeMillis());
            queue.add(msg); //添加到待处理队列
        }
        return res;
    }

    /**
     * 获取一个用户的推送信息
     * method_name: getUserInfos
     * params: [uid]
     * return: java.util.List<com.quakoo.framework.ext.push.model.PushUserInfoPool>
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 15:13
     **/
    @Override
	public List<PushUserInfoPool> getUserInfos(long uid) throws Exception {
		return pushUserInfoPoolDao.getPushUserInfos(uid);
	}

	/**
     * 获取多个用户的推送信息
	 * method_name: getUserInfos
	 * params: [uids]
	 * return: java.util.Map<java.lang.Long,java.util.List<com.quakoo.framework.ext.push.model.PushUserInfoPool>>
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 15:14
	 **/
	@Override
	public Map<Long, List<PushUserInfoPool>> getUserInfos(List<Long> uids)
			throws Exception {
		return pushUserInfoPoolDao.getPushUserInfos(uids);
	}
	
}
