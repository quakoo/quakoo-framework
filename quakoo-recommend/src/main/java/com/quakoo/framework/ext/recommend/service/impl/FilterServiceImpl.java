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
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class FilterServiceImpl implements FilterService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);

    @Resource
    private AbstractRecommendInfo recommendInfo;

    private RedisBloomFilter<Long> bloomFilter;

    private String user_recommended_key_format = "%s_recommend_filter_user_%d_type_%d";

    private String user_recommended_key_day_format = "%s_recommend_filter_user_%d_type_%d_day_%d";

    private SimpleDateFormat daySDF = new SimpleDateFormat("yyyyMMdd");

    private String projectName;

    private int timeout = 60 * 60 * 24 * 30;

    private JedisX cache;

    @Override
    public void afterPropertiesSet() throws Exception {
        projectName = recommendInfo.projectName;
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
        bloomFilter = new RedisBloomFilter<>(cache, 0.0001, 10000);
    }

    @Override
    public void record(long uid, int type, List<Long> aids) throws Exception {
        logger.info("======= filter record uid : " + uid + ",type : " + type + ", size : " + aids.size());
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        bloomFilter.addAll(user_recommended_key, timeout, aids);

        int day = Integer.parseInt(daySDF.format(new Date()));
        String user_recommended_day_key = String.format(user_recommended_key_format, projectName, uid, type, day);
        if(cache.exists(user_recommended_day_key)) {
            Map<String, Object> redisMap = Maps.newHashMap();
            for(long aid : aids) {
                redisMap.put(String.valueOf(aid), true);
            }
            cache.hMultiSetObject(user_recommended_day_key, redisMap);
        }
    }

    @Override
    public Map<Long, Boolean> filter(long uid, int type, List<Long> aids) throws Exception {
        Map<Long, Boolean> res = Maps.newHashMap();
        int day = Integer.parseInt(daySDF.format(new Date()));
        String user_recommended_day_key = String.format(user_recommended_key_format, projectName, uid, type, day);
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        if(!cache.exists(user_recommended_day_key)) {
            res = bloomFilter.containsAll(user_recommended_key, aids);
            Map<String, Object> redisMap = Maps.newHashMap();
            for(Map.Entry<Long, Boolean> entry : res.entrySet()) {
                redisMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            cache.hMultiSetObject(user_recommended_day_key, redisMap);
            cache.expire(user_recommended_day_key, 60 * 60 * 24 + 5);
        } else {
            Map<String, Object> redisDayMap = cache.hGetAllObject(user_recommended_day_key, null);
            List<Long> secondAids = Lists.newArrayList();
            for(long aid : aids) {
                Object redisValue = redisDayMap.get(String.valueOf(aid));
                if(redisValue == null) {
                    secondAids.add(aid);
                } else {
                    res.put(aid, (boolean)redisValue);
                }
            }
            if(secondAids.size() > 0) {
                Map<Long, Boolean> secondRes = bloomFilter.containsAll(user_recommended_key, secondAids);
                res.putAll(secondRes);
                Map<String, Object> redisMap = Maps.newHashMap();
                for(Map.Entry<Long, Boolean> entry : secondRes.entrySet()) {
                    redisMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                cache.hMultiSetObject(user_recommended_day_key, redisMap);
            }
        }
        return res;
    }

    @Override
    public void clear(long uid, int type) throws Exception {
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        bloomFilter.clear(user_recommended_key);

        int day = Integer.parseInt(daySDF.format(new Date()));
        String user_recommended_day_key = String.format(user_recommended_key_format, projectName, uid, type, day);
        cache.delete(user_recommended_day_key);
    }

}
