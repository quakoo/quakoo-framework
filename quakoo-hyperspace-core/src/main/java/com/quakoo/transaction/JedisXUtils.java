package com.quakoo.transaction;

import com.quakoo.baseFramework.redis.JedisBean;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.serialize.ScloudSerializable;
import com.quakoo.space.model.transaction.JedisMethodInfo;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * redis处理(当开启事务时，写操作在commit操作的时候滞后处理，读操作不做特殊处理)
 * class_name: JedisXUtils
 * package: com.s7.space.model.transaction
 * creat_user: lihao
 * creat_date: 2019/4/25
 * creat_time: 11:22
 **/
public class JedisXUtils {

    /**
     * 保存方法信息(事务commit之后，操作写缓存)
     * method_name: saveMethod
     * params: [redisHelp, params]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/4/25
     * creat_time: 11:43
     **/
    private static void saveMethod(RedisHelp redisHelp, Object... params) {
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        JedisMethodInfo methodInfo = new JedisMethodInfo();
        methodInfo.setParams(params);
        methodInfo.setMethodName(methodName);
        redisHelp.getMethodInfos().add(methodInfo);
    }

    public static Boolean exists(JedisX cache, String key) {
        return cache.exists(key);
    }

    public static Long zrankObject(JedisX cache, String key, Object member) {
        return cache.zrankObject(key, member);
    }

    public static Long zrevrankObject(JedisX cache, String key, Object member) {
        return cache.zrevrankObject(key, member);
    }

    public static Map<String, Object> multiGetObject(JedisX cache, List<String> keys, Class<? extends ScloudSerializable> clazz) {
        return cache.multiGetObject(keys, clazz);
    }

    public static Set<Object> zrangeByScoreObject(JedisX cache, String key, double minScore, double maxScore, Class<? extends ScloudSerializable> clazz) {
        return cache.zrangeByScoreObject(key, minScore, maxScore, clazz);
    }

    public static Set<Object> zrangeByScoreObject(JedisX cache, String key, double minScore, double maxScore, int offset, int count, Class<? extends ScloudSerializable> clazz) {
        return cache.zrangeByScoreObject(key, minScore, maxScore, offset, count, clazz);
    }

    public static Set<Object> zrevrangeByScoreObject(JedisX cache, String key, double maxScore, double minScore, int offset, int count, Class<? extends ScloudSerializable> clazz) {
        return cache.zrevrangeByScoreObject(key, maxScore, minScore, offset, count, clazz);
    }

    public static Set<Object> zrevrangeByScoreObject(JedisX cache, String key, double maxScore, double minScore, Class<? extends ScloudSerializable> clazz) {
        return cache.zrevrangeByScoreObject(key, maxScore, minScore, clazz);
    }

    public static Set<Object> zunionstoreRangeByScoreObject(JedisX cache, List<String> keys, double minScore, double maxScore, int offset, int count, Class<? extends ScloudSerializable> clazz) {
        return cache.zunionstoreRangeByScoreObject(keys, minScore, maxScore, offset, count, clazz);
    }

    public static Set<Object> zunionstoreRevrangeByScoreObject(JedisX cache, List<String> keys, double maxScore, double minScore, int offset, int count, Class<? extends ScloudSerializable> clazz) {
        return cache.zunionstoreRevrangeByScoreObject(keys, maxScore, minScore, offset, count, clazz);
    }

    public static Long zaddMultiObject(JedisX cache, String key, Map<Object, Double> memberScores, boolean delay) {
        if(delay) {
            RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
            if(redisHelp != null) {
                saveMethod(redisHelp, key, memberScores);
                return (long)memberScores.size();
            }
        }
        return cache.zaddMultiObject(key, memberScores);
    }

    public static Long expire(JedisX cache, String key, int expireSeconds, boolean delay) {
        if(delay) {
            RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
            if(redisHelp != null) {
                saveMethod(redisHelp, key, expireSeconds);
                return 1l;
            }
        }
        return cache.expire(key, expireSeconds);
    }

    public static String getString(JedisX cache, String key) {
        return cache.getString(key);
    }

    public static Object getObject(JedisX cache, String key, Class<? extends ScloudSerializable> clazz) {
        return cache.getObject(key, clazz);
    }

    public static long delete(JedisX cache, String key) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) {
            saveMethod(redisHelp, key);
            return 1l;
        } else {
            return cache.delete(key);
        }
    }

    public static String setString(JedisX cache, String key, int expireSecond, String value, boolean delay) {
        if(delay) {
            RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
            if(redisHelp != null) { //事务处理
                saveMethod(redisHelp, key, expireSecond, value);
                return "OK";
            }
        }
        return cache.setString(key, expireSecond, value);
    }

    public static String setObject(JedisX cache, String key, int expireSecond, Object value) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) { //事务处理
            saveMethod(redisHelp, key, expireSecond, value);
            return "OK";
        } else {
            return cache.setObject(key, expireSecond, value);
        }
    }

    public static void incrBy(JedisX cache, String key, long step) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) { //事务处理
            saveMethod(redisHelp, key, step);
        } else {
            cache.incrBy(key, step);
        }
    }

    public static void decrBy(JedisX cache, String key, long step) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) { //事务处理
            saveMethod(redisHelp, key, step);
        } else {
            cache.decrBy(key, step);
        }
    }

    public static Long zaddObject(JedisX cache, String key, double score, Object member) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) { //事务处理
            saveMethod(redisHelp, key, score, member);
            return 1l;
        } else {
            return cache.zaddObject(key, score, member);
        }
    }

    public static Long zcard(JedisX cache, String key) {
        return cache.zcard(key);
    }

    public static void zremrangeByRank(JedisX cache, String key, int start, int end) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) { //事务处理
            saveMethod(redisHelp, key, start, end);
        } else {
            cache.zremrangeByRank(key, start, end);
        }
    }

    public static void zremObject(JedisX cache, String key, Object member) {
        RedisHelp redisHelp = (RedisHelp)TransactionSynchronizationManager.getResource("redis_help");
        if(redisHelp != null) { //事务处理
            saveMethod(redisHelp, key, member);
        } else {
            cache.zremObject(key, member);
        }
    }

    public static Set<Object> zrangeObject(JedisX cache, String key, int start, int end, Class<? extends ScloudSerializable> clazz) {
        return cache.zrangeObject(key, start, end, clazz);
    }

    public static Set<Object> zrevrangeObject(JedisX cache, String key, int start, int end, Class<? extends ScloudSerializable> clazz) {
        return cache.zrevrangeObject(key, start, end, clazz);
    }

    public static void main(String[] args) throws Exception {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(300);
        config.setMinIdle(200);
        config.setMaxWaitMillis(1000);
        config.setTestOnBorrow(false);

        JedisBean bean = new JedisBean();
        bean.setMasterAddress("39.107.247.82:6381");
        bean.setPassword("Queke123456");

        JedisX cache = new JedisX(bean, config, 2000);
        RedisHelp redisHelp = new RedisHelp();
        setObject(cache, "aaaaaaa", 1000, "aaaaa");
        setObject(cache, "aaaaaa", 1000, "bbbb");

        for(JedisMethodInfo methodInfo : redisHelp.getMethodInfos()) {
            Object res = MethodUtils.invokeMethod(cache, methodInfo.getMethodName(), methodInfo.getParams());
            System.out.println(res.toString());
        }


        System.exit(1);

    }

}
