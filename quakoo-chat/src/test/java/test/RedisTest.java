package test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.bloom.RedisBloomFilter;
import com.quakoo.baseFramework.redis.JedisBean;
import com.quakoo.baseFramework.redis.JedisX;
import redis.clients.jedis.JedisPoolConfig;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;


public class RedisTest {

    static class TestCallable implements Callable<Void> {
        private String key;
        private List<Integer> values;
        private RedisBloomFilter<Integer> bloomFilter;

        public TestCallable(String key, List<Integer> values, RedisBloomFilter<Integer> bloomFilter) {
            this.key = key;
            this.values = values;
            this.bloomFilter = bloomFilter;
        }

        @Override
        public Void call() throws Exception {
            bloomFilter.addAll(key, 60 * 60, values);
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        JedisPoolConfig queueConfig = new JedisPoolConfig();
        queueConfig.setMaxTotal(50);
        queueConfig.setMaxIdle(25);
        queueConfig.setMinIdle(10);
        queueConfig.setMaxWaitMillis(1000);
        queueConfig.setTestOnBorrow(true);
        queueConfig.setTestWhileIdle(true);
        JedisBean queueInfo = new JedisBean();
        queueInfo.setMasterAddress("47.92.108.189:6384");
        queueInfo.setPassword("Queke123!!!");

        JedisX cache = new JedisX(queueInfo, queueConfig, 5000);

        Object str = cache.getObject("steel_object_com.quakoo.model.PayPassport_id_1", null);
        System.out.println(str.toString());

//        long num = cache.incrBy("intest", 1);
//        System.out.println(num);

//        HotWord a = new HotWord();
//        a.setWord("a");
//        a.setWeight(0.1);
//        Object obj = cache.hGetObject("map", "a", null);
//
//        if(obj == null) {
//            a.setNum(1);
//            cache.hSetObject("map", "a", a);
//        } else {
//            HotWord db = (HotWord) obj;
//            db.setNum(db.getNum() + 1);
//            cache.hSetObject("map", "a", db);
//        }
//        HotWord b = new HotWord();
//        b.setWord("a");
//        b.setWeight(0.1);
//
//        Map<String, Object> map = cache.hMultiGetObject("map", Lists.<String>newArrayList("a", "b"), null);
//
//
//        obj = cache.hGetObject("map", "a", null);
//        if(obj == null) {
//            b.setNum(1);
//            cache.hSetObject("map", "a", b);
//        } else {
//            HotWord db = (HotWord) obj;
//            db.setNum(db.getNum() + 1);
//            cache.hSetObject("map", "a", db);
//        }
//
//        obj = cache.hGetObject("map", "a", null);
//        HotWord db = (HotWord) obj;
//        System.out.println(db.toString());
//        ExecutorService executorService = Executors.newFixedThreadPool(3);
//
//        RedisBloomFilter<Integer> bloomFilter = new RedisBloomFilter<>(cache, 0.0001, 20000);
//        List<Integer> list = Lists.newArrayList(1, 2, 3);
//        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
//        for(int i = 1; i <= 3; i++) {
//            completionService.submit(new TestCallable("test_" +
//                    String.valueOf(i), list, bloomFilter));
//        }
//        for (int i = 0; i < 3; i++) {
//            completionService.take().get();
//        }
//
//        Map<Integer, Boolean> map = bloomFilter.containsAll("test_1", list);
//        System.out.println(map.toString());
//        map = bloomFilter.containsAll("test_2", list);
//        System.out.println(map.toString());
//        map = bloomFilter.containsAll("test_3", list);
//        System.out.println(map.toString());

        //
//        List<Long> params = Lists.newArrayList();
//        for(long i = Long.MAX_VALUE; i > Long.MAX_VALUE - 3000; i--) {
//            params.add(i);
//        }
//        long start = System.currentTimeMillis();
//        bloomFilter.addAll("user1", 60 * 5, params.subList(0, 2999));
//        System.out.println("time : " +(System.currentTimeMillis() - start));
//
//        long start2 = System.currentTimeMillis();
//        Map<Long, Boolean> map = bloomFilter.containsAll("user1", params);
//        System.out.println("time : " +(System.currentTimeMillis() - start2));
//        int num = 0;
//        for(Map.Entry<Long, Boolean> entry : map.entrySet()) {
//           if(entry.getValue()) {
//               num ++;
//           }
//        }
//        System.out.println("true num : " + num);

//        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
//        for(int i = 0 ; i < 10; i++) {
//            long start = System.currentTimeMillis();
//            boolean sign = bloomFilter.contains("user1", (long)i);
//            System.out.println("time : " +(System.currentTimeMillis() - start) + ", " + i + " : " + sign);
//        }
//
//        bloomFilter.addAll("user1", 0, Lists.newArrayList(2l, 3l));
//
//        sign = bloomFilter.contains("user1", 2l);
//        System.out.println("2 : " + sign);
//        sign = bloomFilter.contains("user1", 3l);
//        System.out.println("3 : " + sign);

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
