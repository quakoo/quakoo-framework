package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.bloom.RedisBloomFilter;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.service.FilterService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

@Data
public class FilterServiceImpl implements FilterService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);

    @Resource
    private AbstractRecommendInfo recommendInfo;

    private RedisBloomFilter<Long> bloomFilter;

    private String user_recommended_key_format = "%s_recommend_filter_user_%d_type_%d_week_%s";

    private String user_recommended_temp_key_format = "%s_recommend_temp_filter_user_%d_type_%d";

//    private SimpleDateFormat daySDF = new SimpleDateFormat("yyyyMMdd");

    private String projectName;

    private static int deadline_week = 4; //4周周期

    private int timeout = 7 * 60 * 60 * 24;

    private JedisX cache;

    @Override
    public void afterPropertiesSet() throws Exception {
        projectName = recommendInfo.projectName;
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
        bloomFilter = new RedisBloomFilter<>(cache, 0.0001, 10000);
    }

    private List<String> getRecordKeys(long uid, int type) {
        List<String> res = Lists.newArrayList();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String week = calendar.get(Calendar.YEAR) + "" + calendar.get(Calendar.WEEK_OF_YEAR);
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type, week);
        res.add(user_recommended_key);
        for(int i = 1; i < deadline_week; i++) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            week = calendar.get(Calendar.YEAR) + "" + calendar.get(Calendar.WEEK_OF_YEAR);
            user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type, week);
            res.add(user_recommended_key);
        }
        return res;
    }

    private String getKey(long uid, int type) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String week = calendar.get(Calendar.YEAR) + "" + calendar.get(Calendar.WEEK_OF_YEAR);
        String res = String.format(user_recommended_key_format, projectName, uid, type, week);
        return res;
    }

    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        System.out.println(calendar.get(Calendar.YEAR) + "" + calendar.get(Calendar.WEEK_OF_YEAR));
        List<String> weeks = Lists.newArrayList();
        for(int i = 1; i < deadline_week; i++) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            weeks.add(calendar.get(Calendar.YEAR) + "" + calendar.get(Calendar.WEEK_OF_YEAR));
        }
        System.out.println(weeks.toString());
    }

    @Override
    public void record(long uid, int type, List<Long> aids) throws Exception {
        logger.info("======= filter record uid : " + uid + ",type : " + type + ", size : " + aids.size());
//        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
//        bloomFilter.addAll(user_recommended_key, timeout, aids);

        List<String> keys = getRecordKeys(uid, type);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(recommendInfo.executorService);
        for(int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            int oneTimeout = timeout * (i + 1) + 5000;
            completionService.submit(new RecordCallable(bloomFilter, key, oneTimeout, aids));
        }
        for (int i = 0; i < keys.size(); i++) {
            completionService.take().get();
        }
        String user_recommended_temp_key = String.format(user_recommended_temp_key_format, projectName, uid, type);
        if(cache.exists(user_recommended_temp_key)) {
            Map<String, Object> redisMap = Maps.newHashMap();
            for(long aid : aids) {
                redisMap.put(String.valueOf(aid), true);
            }
            cache.hMultiSetObject(user_recommended_temp_key, redisMap);
        }
    }

    @Override
    public Map<Long, Boolean> filter(long uid, int type, List<Long> aids) throws Exception {
        Map<Long, Boolean> res = Maps.newHashMap();
        String user_recommended_temp_key = String.format(user_recommended_temp_key_format, projectName, uid, type);
//        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        String key = getKey(uid, type);
        if(!cache.exists(user_recommended_temp_key)) {
            res = bloomFilter.containsAll(key, aids);
            Map<String, Object> redisMap = Maps.newHashMap();
            for(Map.Entry<Long, Boolean> entry : res.entrySet()) {
                redisMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            cache.hMultiSetObject(user_recommended_temp_key, redisMap);
            cache.expire(user_recommended_temp_key, 60 * 60 * 24 * 3); //3天
        } else {
            List<String> aidStrs = Lists.newArrayList();
            for(long aid : aids) {
                aidStrs.add(String.valueOf(aid));
            }
            Map<String, Object> redisTempMap = cache.hMultiGetObject(user_recommended_temp_key, aidStrs, null);
            List<Long> secondAids = Lists.newArrayList();
            for(long aid : aids) {
                Object redisValue = redisTempMap.get(String.valueOf(aid));
                if(redisValue == null) {
                    secondAids.add(aid);
                } else {
                    res.put(aid, (boolean)redisValue);
                }
            }
            if(secondAids.size() > 0) {
                Map<Long, Boolean> secondRes = bloomFilter.containsAll(key, secondAids);
                res.putAll(secondRes);
                Map<String, Object> redisMap = Maps.newHashMap();
                for(Map.Entry<Long, Boolean> entry : secondRes.entrySet()) {
                    redisMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                cache.hMultiSetObject(user_recommended_temp_key, redisMap);
            }
        }
        return res;
    }

    @Override
    public void clear(long uid, int type) throws Exception {
        String key = getKey(uid, type);
//        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        bloomFilter.clear(key);

        String user_recommended_temp_key = String.format(user_recommended_temp_key_format, projectName, uid, type);
        cache.delete(user_recommended_temp_key);
    }

    class RecordCallable implements Callable<Void> {
        private RedisBloomFilter<Long> bloomFilter;
        private String key;
        private int timeout;
        private List<Long> aids;

        public RecordCallable(RedisBloomFilter<Long> bloomFilter, String key, int timeout, List<Long> aids) {
            this.bloomFilter = bloomFilter;
            this.key = key;
            this.timeout = timeout;
            this.aids = aids;
        }

        @Override
        public Void call() throws Exception {
            bloomFilter.addAll(key, timeout, aids);
            return null;
        }
    }

}
