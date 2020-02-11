package com.quakoo.framework.ext.recommend.dao.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.dao.BaseDao;
import com.quakoo.framework.ext.recommend.dao.HotWordDao;
import com.quakoo.framework.ext.recommend.model.HotWord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HotWordDaoImpl extends BaseDao implements HotWordDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(HotWordDaoImpl.class);

    private JedisX cache;

    private SimpleDateFormat hourSDF = new SimpleDateFormat("yyyyMMddHH");

    private final static String hot_word_queue_key = "%s_hot_word_queue_%d";

    private final static String hot_word_key = "%s_hot_word";

    private ResultSetExtractor<HotWord> resultSetExtractor = new ResultSetExtractor<HotWord>() {
        @Override
        public HotWord extractData(ResultSet rs) throws SQLException, DataAccessException {
            if(rs.next()){
                HotWord hotWord = new HotWord();
                hotWord.setId(rs.getLong("id"));
                hotWord.setWord(rs.getString("word"));
                hotWord.setWeight(rs.getDouble("weight"));
                hotWord.setNum(rs.getInt("num"));
                hotWord.setSort(rs.getLong("sort"));
                return hotWord;
            } else
                return null;
        }
    };

    private RowMapper<HotWord> rowMapper = new RowMapper<HotWord>() {
        @Override
        public HotWord mapRow(ResultSet rs, int i) throws SQLException {
            HotWord hotWord = new HotWord();
            hotWord.setId(rs.getLong("id"));
            hotWord.setWord(rs.getString("word"));
            hotWord.setWeight(rs.getDouble("weight"));
            hotWord.setNum(rs.getInt("num"));
            hotWord.setSort(rs.getLong("sort"));
            return hotWord;
        }
    };

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws DataAccessException {
        String sql = "select * from hot_word where 1 = 1 ";
        String sqlCount = "select count(id) from hot_word where 1 = 1 ";
        if(StringUtils.isNotBlank(word)) {
            sql += " and word like '%" + word + "%' ";
            sqlCount += " and word like '%" + word + "%' ";
        }
        String limit = " limit %d, %d ";
        sql = sql + " order by `sort` desc";
        sql = sql + String.format(limit, (pager.getPage() - 1) * pager.getSize(), pager.getSize());
        List<HotWord> data = this.jdbcTemplate.query(sql, rowMapper);
        int count = this.jdbcTemplate.queryForObject(sqlCount, Integer.class);
        pager.setData(data);
        pager.setTotalCount(count);
        return pager;
    }

    @Override
    public List<String> getLastHotWords() throws DataAccessException {
        String key = String.format(hot_word_key, recommendInfo.projectName);
        if (cache.exists(key)) {
            Set<String> set = cache.zrevrangeString(key, 0, 10);
            return Lists.newArrayList(set);
        } else {
            String sql = "select * from hot_word order by sort desc limit 0, 10";
            List<HotWord> hotWords = this.jdbcTemplate.query(sql, rowMapper);
            Map<String, Double> redisMap = Maps.newHashMap();
            List<String> res = Lists.newArrayList();
            for(HotWord one : hotWords) {
                redisMap.put(one.getWord(), (double)one.getSort());
                res.add(one.getWord());
            }
            cache.zaddMultiString(key, redisMap);
            return res;
        }
    }

    @Override
    public boolean delete(long id) throws DataAccessException {
        String sql = "select * from hot_word where id = %d";
        HotWord dbHotWord = this.jdbcTemplate.query(String.format(sql, id), resultSetExtractor);
        if (dbHotWord != null) {
            sql = "delete from hot_word where id = %d";
            sql = String.format(sql, id);
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(hot_word_key, recommendInfo.projectName);
                cache.zremString(key, dbHotWord.getWord());
            }
            return res > 0;
        }
        return false;
    }

    @Override
    public int insert(List<HotWord> list) throws DataAccessException {
        int res = 0;
        String sqlPrev = "insert ignore into hot_word (word, weight, num, sort) values ";
        String sqlValueFormat = "('%s', %s, %d, %d)";
        List<String> sqlValueList = Lists.newArrayList();
        for (HotWord one : list) {
            String sqlValue = String.format(sqlValueFormat, one.getWord(), one.getWeight(), one.getNum(), one.getSort());
            sqlValueList.add(sqlValue);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues;
        long startTime = System.currentTimeMillis();
        res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        String key = String.format(hot_word_key, recommendInfo.projectName);
        if (res == list.size()) {
            if (cache.exists(key)) {
                Map<String, Double> redisMap = Maps.newHashMap();
                for(HotWord one : list) {
                    redisMap.put(one.getWord(), (double)one.getSort());
                }
                cache.zaddMultiString(key, redisMap);
                long len = cache.zcard(key);
                if(len > 10) {
                    int end = (int) (len - 10 - 1);
                    cache.zremrangeByRank(key, 0, end);
                }
            }
        } else {
            cache.delete(key);
        }

        return res;
    }

    @Override
    public void record(List<HotWord> hotWords) throws DataAccessException {
        long timeFormat = Long.parseLong(hourSDF.format(new Date()));
        String key = String.format(hot_word_queue_key, recommendInfo.projectName, timeFormat);
        List<Object> list = Lists.newArrayList();
        list.addAll(hotWords);
        cache.piprpushObject(key, list);
        cache.expire(key, 60 * 60 * 2);
    }

    @Override
    public List<HotWord> getRecords(long timeFormat) throws DataAccessException {
        String key = String.format(hot_word_queue_key, recommendInfo.projectName, timeFormat);
        if(cache.exists(key)) {
            List<Object> list = cache.lrangeAndDelObject(key, 0, Integer.MAX_VALUE, null);
            List<HotWord> hotWords = Lists.newArrayList();
            for(Object one : list) {
                HotWord hotWord = (HotWord)one;
                hotWords.add(hotWord);
            }
            return hotWords;
        } else {
            System.out.println(" ===== no hot key");
        }
        return Lists.newArrayList();
    }



}
