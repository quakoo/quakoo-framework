package com.quakoo.framework.ext.recommend.service.impl;

import com.quakoo.baseFramework.bloom.RedisBloomFilter;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.service.FilterService;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Data
public class FilterServiceImpl implements FilterService, InitializingBean {

    @Resource
    private AbstractRecommendInfo recommendInfo;

    private RedisBloomFilter<Long> bloomFilter;

    private String user_recommended_key_format = "%s_recommend_filter_user_%d_type_%d";

    private String projectName;

    private int timeout = 60 * 60 * 24 * 30;

    @Override
    public void afterPropertiesSet() throws Exception {
        projectName = recommendInfo.projectName;
        JedisX cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
        bloomFilter = new RedisBloomFilter<>(cache, 0.0001, 10000);
    }

    @Override
    public void record(long uid, int type, List<Long> aids) throws Exception {
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        bloomFilter.addAll(user_recommended_key, timeout, aids);
    }

    @Override
    public Map<Long, Boolean> filter(long uid, int type, List<Long> aids) throws Exception {
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        Map<Long, Boolean> res = bloomFilter.containsAll(user_recommended_key, aids);
        return res;
    }

    @Override
    public void clear(long uid, int type) throws Exception {
        String user_recommended_key = String.format(user_recommended_key_format, projectName, uid, type);
        bloomFilter.clear(user_recommended_key);
    }

}
