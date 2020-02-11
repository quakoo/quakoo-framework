package com.quakoo.framework.ext.recommend.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import com.quakoo.framework.ext.recommend.dao.BaseDao;
import com.quakoo.framework.ext.recommend.dao.StopWordDao;
import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import com.quakoo.framework.ext.recommend.model.StopWord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class StopWordDaoImpl extends BaseDao implements StopWordDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(StopWordDaoImpl.class);

    private JedisX cache;

    private final static String stop_word_key = "%s_stop_word";

    private ResultSetExtractor<StopWord> resultSetExtractor = new ResultSetExtractor<StopWord>() {
        @Override
        public StopWord extractData(ResultSet rs) throws SQLException, DataAccessException {
            if(rs.next()){
                StopWord stopWord = new StopWord();
                stopWord.setId(rs.getLong("id"));
                stopWord.setWord(rs.getString("word"));
                return stopWord;
            } else
                return null;
        }
    };

    private RowMapper<StopWord> rowMapper = new RowMapper<StopWord>() {
        @Override
        public StopWord mapRow(ResultSet rs, int i) throws SQLException {
            StopWord stopWord = new StopWord();
            stopWord.setId(rs.getLong("id"));
            stopWord.setWord(rs.getString("word"));
            return stopWord;
        }
    };

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws DataAccessException {
        String sql = "select * from stop_word where 1 = 1 ";
        String sqlCount = "select count(id) from stop_word where 1 = 1 ";
        if(StringUtils.isNotBlank(word)) {
            sql += " and word like '%" + word + "%' ";
            sqlCount += " and word like '%" + word + "%' ";
        }
        String limit = " limit %d, %d ";
        sql = sql + " order by `id` desc";
        sql = sql + String.format(limit, (pager.getPage() - 1) * pager.getSize(), pager.getSize());
        List<StopWord> data = this.jdbcTemplate.query(sql, rowMapper);
        int count = this.jdbcTemplate.queryForObject(sqlCount, Integer.class);
        pager.setData(data);
        pager.setTotalCount(count);
        return pager;
    }

    @Override
    public Set<String> loadStopWords() throws DataAccessException {
        String key = String.format(stop_word_key, recommendInfo.projectName);
        if (cache.exists(key)) {
            Set<String> res = cache.smemberString(key);
            return res;
        } else {
            String sql = "select * from stop_word";
            List<StopWord> list = this.jdbcTemplate.query(sql, rowMapper);
            Set<String> res = Sets.newLinkedHashSet();
            for(StopWord one : list) {
                res.add(one.getWord());
            }
            if(res.size() > 0) {
                cache.smultiAddString(key, res.toArray(new String[]{}));
                cache.expire(key, AbstractRecommendInfo.redis_overtime_long);
            }
            return res;
        }
    }

    @Override
    public int insert(List<StopWord> stopWords) throws DataAccessException {
        int res = 0;
        String sqlPrev = "insert ignore into stop_word (word) values ";
        String sqlValueFormat = "(?)";
        List<Object> params = Lists.newArrayList();
        List<String> sqlValueList = Lists.newArrayList();
        for (StopWord stopWord : stopWords) {
            sqlValueList.add(sqlValueFormat);
            params.add(stopWord.getWord());
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues;
        long startTime = System.currentTimeMillis();
        res = this.jdbcTemplate.update(sql, params.toArray());
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        String key = String.format(stop_word_key, recommendInfo.projectName);
        if (res == stopWords.size()) {
            if (cache.exists(key)) {
                Set<String> redisSet = Sets.newHashSet();
                for(StopWord stopWord : stopWords) {
                    redisSet.add(stopWord.getWord());
                }
                cache.smultiAddString(key, redisSet.toArray(new String[]{}));
            }
        } else {
            cache.delete(key);
        }
        return res;
    }

    @Override
    public boolean update(StopWord stopWord) throws DataAccessException {
        String sql = "select * from stop_word where id = %d";
        StopWord dbStopWord = this.jdbcTemplate.query(String.format(sql, stopWord.getId()), resultSetExtractor);
        if (dbStopWord != null) {
            sql = "update stop_word set word = '%s' where id = %d";
            sql = String.format(sql, stopWord.getWord(), stopWord.getId());
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(stop_word_key, recommendInfo.projectName);
                cache.sremString(key, dbStopWord.getWord());
                cache.saddString(key, stopWord.getWord());
            }
            return res > 0;
        }
        return false;
    }

    @Override
    public boolean delete(long id) throws DataAccessException {
        String sql = "select * from stop_word where id = %d";
        StopWord dbStopWord = this.jdbcTemplate.query(String.format(sql, id), resultSetExtractor);
        if (dbStopWord != null) {
            sql = "delete from stop_word where id = %d";
            sql = String.format(sql, id);
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(stop_word_key, recommendInfo.projectName);
                cache.sremString(key, dbStopWord.getWord());
            }
            return res > 0;
        }
        return false;
    }

}