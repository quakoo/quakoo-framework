package com.quakoo.framework.ext.recommend;

import com.quakoo.baseFramework.property.PropertyLoader;
import com.quakoo.baseFramework.redis.JedisBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import redis.clients.jedis.JedisPoolConfig;

public class AbstractRecommendInfo implements InitializingBean {

    private PropertyLoader propertyLoader = PropertyLoader.getInstance("recommend.properties");

    public static final int type_time = 1;
    public static final int type_hot_word = 2;
    public static final int type_item_cf = 3;


    public String projectName; //项目名
    public String lockZkAddress; //分布式锁
    public String distributedZkAddress; //分布式配置地址

    public static final int lock_timeout = 60000;
    public static final int session_timout = 5000;

    public static final int redis_overtime_long = 60 * 60 * 24 * 3;
    public static final int redis_overtime = 60 * 10;

    public JedisPoolConfig redisConfig;
    public JedisBean redisInfo;

    public int itemCFStep = 6;
    public int timeStep = 2;
    public int hotWordStep = 2;
    public int cacheMultiple = 5;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.projectName = propertyLoader.getProperty("recommend.project.name");
        this.lockZkAddress = propertyLoader.getProperty("recommend.lock.zk.address");
        this.distributedZkAddress = propertyLoader.getProperty("recommend.distributed.zk.address");

        this.itemCFStep = propertyLoader.getIntProperty("recommend.item.cf.step");
        this.timeStep = propertyLoader.getIntProperty("recommend.time.step");
        this.hotWordStep = propertyLoader.getIntProperty("recommend.hot.word.step");
        this.cacheMultiple = propertyLoader.getIntProperty("recommend.cache.multiple");

        String redisAddress = propertyLoader.getProperty("recommend.redis.address");
        String redisPassword = propertyLoader.getProperty("recommend.redis.password");
        if (StringUtils.isBlank(redisAddress) || StringUtils.isBlank(redisPassword)) {
            throw new IllegalStateException("缓存配置不能为空");
        }

        redisConfig = new JedisPoolConfig();
        redisConfig.setMaxTotal(300);
        redisConfig.setMaxIdle(200);
        redisConfig.setMinIdle(20);
        redisConfig.setMaxWaitMillis(1000);
        redisConfig.setTestOnBorrow(false);

        redisInfo = new JedisBean();
        redisInfo.setMasterAddress(redisAddress);
        redisInfo.setPassword(redisPassword);
    }
}
