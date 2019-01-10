package com.quakoo.framework.ext.push.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.push.bean.PushUserInfoMsg;
import com.quakoo.framework.ext.push.dao.PushUserInfoPoolDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;
import com.quakoo.framework.ext.push.model.PushUserQueue;
import com.quakoo.framework.ext.push.service.BaseService;
import com.quakoo.framework.ext.push.service.PushUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class PushUserServiceImpl extends BaseService implements PushUserService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(PushUserServiceImpl.class);

    private final static int handle_num = 50;

    private final static int handle_expire_time = 1000 * 60 * 5;

    @Resource
    private PushUserInfoPoolDao pushUserInfoPoolDao;

    @Resource
    private PushUserQueueDao pushUserQueueDao;

    private static volatile LinkedBlockingQueue<PushUserInfoMsg> queue = new LinkedBlockingQueue<PushUserInfoMsg>();

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
                pushUserInfoPoolDao.insert(Lists.newArrayList(registMap.values()));
                pushUserQueueDao.insert(Lists.newArrayList(registMap.keySet()));
            }
            if(logoutList.size() > 0) {
                pushUserInfoPoolDao.clear(logoutList);
                pushUserQueueDao.delete(logoutList);
            }
        }

        @Override
        public void run() {
            List<PushUserInfoMsg> batchList= Lists.newArrayList();
            while(true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    PushUserInfoMsg msg = queue.take();
                    batchList.add(msg);
                    if(batchList.size() >= handle_num) {
                        handle(batchList);
                        batchList.clear();
                    } else {
                        List<PushUserInfoMsg> list = Lists.newArrayList();
                        for(Iterator<PushUserInfoMsg> it = batchList.iterator(); it.hasNext();) {
                            PushUserInfoMsg one = it.next();
                            if(currentTime - one.getTime() > handle_expire_time) {
                                list.add(one);
                                it.remove();
                            }
                        }
                        if(list.size() > 0) {
                            handle(list);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

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
        boolean res = pushUserInfoPoolDao.cache_insert(pool);
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
            queue.add(msg);
        }
        return res;
    }

    @Override
    public boolean logoutUserInfo(long uid) throws Exception {
        boolean res = pushUserInfoPoolDao.cache_clear(uid);
        if(res) {
            PushUserInfoMsg msg = new PushUserInfoMsg();
            msg.setType(PushUserInfoMsg.type_logout);
            msg.setUid(uid);
            msg.setTime(System.currentTimeMillis());
            queue.add(msg);
        }
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
