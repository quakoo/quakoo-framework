package com.quakoo.framework.ext.recommend.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import com.quakoo.framework.ext.recommend.dao.BaseDao;
import com.quakoo.framework.ext.recommend.dao.PortraitItemCFDao;
import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PortraitItemCFDaoImpl extends BaseDao implements PortraitItemCFDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(PortraitItemCFDaoImpl.class);

    private JedisX cache;

    private final static String portrait_item_cf_key_format = "%s_portrait_item_cf_user_%d";

    private RowMapper<PortraitItemCF> rowMapper = new RowMapper<PortraitItemCF>() {
        @Override
        public PortraitItemCF mapRow(ResultSet rs, int i) throws SQLException {
            PortraitItemCF portrait = new PortraitItemCF();
            portrait.setUid(rs.getLong("uid"));
            String wordsStr = rs.getString("words");
            List<PortraitWord> words = JsonUtils.fromJson(wordsStr, new TypeReference<List<PortraitWord>>() {});
            portrait.setWords(words);
            portrait.setUtime(rs.getLong("utime"));
            return portrait;
        }
    };

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public int update(List<PortraitItemCF> list) throws DataAccessException {
        long utime = System.currentTimeMillis();
        String sql = "update portrait_item_cf set words = case uid %s end, utime = %d where uid in (%s)";
        String sqlValueFormat = "when %d then '%s'";
        List<Long> uids = Lists.newArrayList();
        List<String> sqlValues = Lists.newArrayList();
        for(PortraitItemCF one : list) {
            one.setUtime(utime);
            sqlValues.add(String.format(sqlValueFormat, one.getUid(), JsonUtils.toJson(one.getWords())));
            uids.add(one.getUid());
        }
        sql = String.format(sql, StringUtils.join(sqlValues, " "), utime, StringUtils.join(uids, ","));
        long startTime = System.currentTimeMillis();
        int res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        if(res == list.size()) {
            Map<String,  Object> redisMap = Maps.newHashMap();
            for(PortraitItemCF one : list) {
                String key = String.format(portrait_item_cf_key_format, recommendInfo.projectName, one.getUid());
                redisMap.put(key, one);
            }
            cache.multiSetObject(redisMap, AbstractRecommendInfo.redis_overtime_long);
        } else {
            List<String> redisKeys = Lists.newArrayList();
            for(PortraitItemCF one : list) {
                String key = String.format(portrait_item_cf_key_format, recommendInfo.projectName, one.getUid());
                redisKeys.add(key);
            }
            cache.multiDelete(redisKeys);
        }
        return res;
    }

    @Override
    public PortraitItemCF load(long uid) throws DataAccessException {
        String key = String.format(portrait_item_cf_key_format, recommendInfo.projectName, uid);
        Object obj = cache.getObject(key, null);
        if(obj != null) {
            return (PortraitItemCF) obj;
        } else {
            String sql = "select * from portrait_item_cf where uid = %d";
            sql = String.format(sql, uid);
            long startTime = System.currentTimeMillis();
            PortraitItemCF portraitItemCF = this.jdbcTemplate.queryForObject(sql, rowMapper);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if(portraitItemCF != null) {
                cache.setObject(key, AbstractRecommendInfo.redis_overtime_long, portraitItemCF);
            }
            return portraitItemCF;
        }
    }

    @Override
    public List<PortraitItemCF> load(List<Long> uids) throws DataAccessException {
        List<String> object_key_list = Lists.newArrayList();
        Map<Long, String> id_key_map = Maps.newHashMap();
        for(long uid : uids){
            String key = String.format(portrait_item_cf_key_format, recommendInfo.projectName, uid);
            object_key_list.add(key);
            id_key_map.put(uid, key);
        }
        Map<Long, PortraitItemCF> res_map = Maps.newHashMap();
        Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
        List<Long> non_ids = Lists.newArrayList();
        for(long uid : uids){
            Object obj = redis_map.get(id_key_map.get(uid));
            if(null == obj){
                non_ids.add(uid);
            } else {
                res_map.put(uid, (PortraitItemCF) obj);
            }
        }
        if(non_ids.size() > 0){
            String sql = "select * from portrait_item_cf where uid in (%s)";
            sql = String.format(sql, StringUtils.join(non_ids, ","));
            long startTime = System.currentTimeMillis();
            List<PortraitItemCF> list = this.jdbcTemplate.query(sql, rowMapper);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            Map<String, Object> redis_insert_map = Maps.newHashMap();
            for(PortraitItemCF portrait : list){
                long uid = portrait.getUid();
                res_map.put(uid, portrait);
                redis_insert_map.put(String.format(portrait_item_cf_key_format, recommendInfo.projectName, uid), portrait);
            }
            cache.multiSetObject(redis_insert_map, AbstractRecommendInfo.redis_overtime_long);
        }
        List<PortraitItemCF> res = Lists.newArrayList();
        for(long uid : uids){
            PortraitItemCF portrait = res_map.get(uid);
            if(portrait != null) res.add(portrait);
        }
        return res;
    }

    @Override
    public int insert(List<PortraitItemCF> list) throws DataAccessException {
        int res = 0;
        String sqlPrev = "insert into portrait_item_cf (uid, words, utime) values ";
        String sqlValueFormat = "(%d, '%s', %d)";
        List<String> sqlValueList = Lists.newArrayList();
        long utime = System.currentTimeMillis();
        for (PortraitItemCF portrait : list) {
            String words = JsonUtils.toJson(portrait.getWords());
            sqlValueList.add(String.format(sqlValueFormat, portrait.getUid(), words, utime));
            portrait.setUtime(utime);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues;
        long startTime = System.currentTimeMillis();
        res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        if(res == list.size()) {
            Map<String,  Object> redisMap = Maps.newHashMap();
            for(PortraitItemCF portrait : list) {
                String key = String.format(portrait_item_cf_key_format, recommendInfo.projectName, portrait.getUid());
                redisMap.put(key, portrait);
            }
            cache.multiSetObject(redisMap, AbstractRecommendInfo.redis_overtime_long);
        }
        return res;
    }

}
