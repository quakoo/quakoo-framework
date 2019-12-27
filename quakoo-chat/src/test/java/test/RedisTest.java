package test;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.bloom.RedisBloomFilter;
import com.quakoo.baseFramework.redis.JedisBean;
import com.quakoo.baseFramework.redis.JedisX;
import redis.clients.jedis.JedisPoolConfig;


public class RedisTest {

    public static void main(String[] args) {
        JedisPoolConfig queueConfig = new JedisPoolConfig();
        queueConfig.setMaxTotal(50);
        queueConfig.setMaxIdle(25);
        queueConfig.setMinIdle(10);
        queueConfig.setMaxWaitMillis(1000);
        queueConfig.setTestOnBorrow(true);
        queueConfig.setTestWhileIdle(true);
        JedisBean queueInfo = new JedisBean();
        queueInfo.setMasterAddress("47.56.109.24:6380");
        queueInfo.setPassword("Queke123456");

        JedisX cache = new JedisX(queueInfo, queueConfig, 5000);

        RedisBloomFilter<Long> bloomFilter = new RedisBloomFilter<>(cache, 0.0001, 2000);
        boolean sign = bloomFilter.contains("user1", 1l);
        System.out.println(sign);
        bloomFilter.add("user1", 60 * 60, 1l);

        sign = bloomFilter.contains("user1", 1l);
        System.out.println(sign);

        bloomFilter.addAll("user1", 0, Lists.newArrayList(2l, 3l));

        sign = bloomFilter.contains("user1", 2l);
        System.out.println("2 : " + sign);
        sign = bloomFilter.contains("user1", 3l);
        System.out.println("3 : " + sign);

//        Set<String> set  = cache.keys("quakooChat_many_chat_object_uid_*");
//        List<List<String>> list = Lists.partition(Lists.newArrayList(set),100);
//        for(List<String> one : list) {
//            System.out.println(one);
//            cache.multiDelete(one);
//        }
//        System.out.println(set.size());
//        cache.zremrangeByScore("test", 0, 1.2d);

//        Set<Object> set = cache.zrangeByScoreObject("quakooChat_hot_user_stream_uid_22368", 0, Double.MAX_VALUE
//        ,null);
//        for(Object o : set) {
//            UserStream us = (UserStream) o;
//            if(us.getMid() == 124412223) System.out.println(us.toString());
//        }

//        cache.zaddString("test", 0.1d, "a");
//        cache.zaddString("test", 0.2d, "b");
//
//        Set<String> set = cache.zrangeByScoreString("test", 0, Double.MAX_VALUE);
//        for(String one : set) {
//            System.out.println(one);
//        }
//        List<RedisSortData.RedisKeySortMemObj> requests = Lists.newArrayList();
//        RedisSortData.RedisKeySortMemObj one = new RedisSortData.RedisKeySortMemObj("test", "a", 0.1d);
//        requests.add(one);
//        RedisSortData.RedisKeySortMemObj two = new RedisSortData.RedisKeySortMemObj("test", "b", 0.2d);
//        requests.add(two);
//        cache.pipZaddObject(requests);
//
//        Set<Object> set = cache.zrangeByScoreObject("test", 0, Double.MAX_VALUE, null);
//        for (Object obj : set) {
//            System.out.println((String)obj);
//        }
//        cache.expire("test", 15);
        System.exit(1);
    }

}
