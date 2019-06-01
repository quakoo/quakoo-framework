package com.quakoo.framework.ext.push.model;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.redis.JedisBean;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.push.model.constant.Brand;
import com.quakoo.framework.ext.push.model.constant.Platform;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 推送用户信息
 * class_name: PushUserInfoPool
 * package: com.quakoo.framework.ext.push.model
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 12:02
 **/
public class PushUserInfoPool implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private long uid; // #b# @hk@ ^nn^
	
	private int platform; // #t# @hk@ ^nn^
	
	private int brand; // #t# @hk@ ^nn^
	
	private String sessionId; // #v200# @hk@ ^nn^
	
	private String iosToken; // #v100# ^n^

    private String huaWeiToken;

    private String meiZuPushId;
	
	private long activeTime; // #b# ^nn 0^

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public int getPlatform() {
		return platform;
	}

	public void setPlatform(int platform) {
		this.platform = platform;
	}

	public int getBrand() {
		return brand;
	}

	public void setBrand(int brand) {
		this.brand = brand;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public long getActiveTime() {
		return activeTime;
	}

	public void setActiveTime(long activeTime) {
		this.activeTime = activeTime;
	}

	public String getIosToken() {
		return iosToken;
	}

	public void setIosToken(String iosToken) {
		this.iosToken = iosToken;
	}

    public String getHuaWeiToken() {
        return huaWeiToken;
    }

    public void setHuaWeiToken(String huaWeiToken) {
        this.huaWeiToken = huaWeiToken;
    }

    public String getMeiZuPushId() {
        return meiZuPushId;
    }

    public void setMeiZuPushId(String meiZuPushId) {
        this.meiZuPushId = meiZuPushId;
    }

    @Override
    public String toString() {
        return "PushUserInfoPool{" +
                "uid=" + uid +
                ", platform=" + platform +
                ", brand=" + brand +
                ", sessionId='" + sessionId + '\'' +
                ", iosToken='" + iosToken + '\'' +
                ", huaWeiToken='" + huaWeiToken + '\'' +
                ", meiZuPushId='" + meiZuPushId + '\'' +
                ", activeTime=" + activeTime +
                '}';
    }

    public static void main(String[] args) {
        JedisPoolConfig queueConfig = new JedisPoolConfig();
        queueConfig.setMaxTotal(50);
        queueConfig.setMaxIdle(25);
        queueConfig.setMinIdle(10);
        queueConfig.setMaxWaitMillis(1000);
        queueConfig.setTestOnBorrow(true);
        queueConfig.setTestWhileIdle(true);
        JedisBean queueInfo = new JedisBean();
        queueInfo.setMasterAddress("47.107.155.86:6384");
        queueInfo.setPassword("Queke123!!!");

        JedisX cache = new JedisX(queueInfo, queueConfig, 5000);

        List<PushUserInfoPool> res = Lists.newArrayList();
        Set<Object> set = cache.smemberObject("minglian_push_user_info_pool_6782833", null);
        if(null != set && set.size() > 0){
            for(Object one : set){
                res.add((PushUserInfoPool) one);
            }
        }

        for(PushUserInfoPool one : res) {
            System.out.println(one.toString());
        }
    }
}
