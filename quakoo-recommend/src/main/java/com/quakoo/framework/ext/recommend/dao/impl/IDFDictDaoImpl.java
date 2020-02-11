package com.quakoo.framework.ext.recommend.dao.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.dao.BaseDao;
import com.quakoo.framework.ext.recommend.dao.IDFDictDao;
import com.quakoo.framework.ext.recommend.model.HotWord;
import com.quakoo.framework.ext.recommend.model.IDFDict;
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
import java.util.Map;


public class IDFDictDaoImpl extends BaseDao implements IDFDictDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(IDFDictDaoImpl.class);

    private JedisX cache;

    private final static String idf_dict_key = "%s_idf_dict";

    private ResultSetExtractor<IDFDict> resultSetExtractor = new ResultSetExtractor<IDFDict>() {
        @Override
        public IDFDict extractData(ResultSet rs) throws SQLException, DataAccessException {
            if(rs.next()){
                IDFDict res = new IDFDict();
                res.setId(rs.getLong("id"));
                res.setWord(rs.getString("word"));
                res.setWeight(rs.getDouble("weight"));
                return res;
            } else
                return null;
        }
    };

    private RowMapper<IDFDict> rowMapper = new RowMapper<IDFDict>() {
        @Override
        public IDFDict mapRow(ResultSet rs, int i) throws SQLException {
            IDFDict res = new IDFDict();
            res.setId(rs.getLong("id"));
            res.setWord(rs.getString("word"));
            res.setWeight(rs.getDouble("weight"));
            return res;
        }
    };

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws DataAccessException {
        String sql = "select * from idf_dict where 1 = 1 ";
        String sqlCount = "select count(id) from idf_dict where 1 = 1 ";
        if(StringUtils.isNotBlank(word)) {
            sql += " and word like '%" + word + "%' ";
            sqlCount += " and word like '%" + word + "%' ";
        }
        String limit = " limit %d, %d ";
        sql = sql + " order by `id` desc";
        sql = sql + String.format(limit, (pager.getPage() - 1) * pager.getSize(), pager.getSize());
        List<IDFDict> data = this.jdbcTemplate.query(sql, rowMapper);
        int count = this.jdbcTemplate.queryForObject(sqlCount, Integer.class);
        pager.setData(data);
        pager.setTotalCount(count);
        return pager;
    }

    @Override
    public Map<String, Double> loadIDFMap() throws DataAccessException {
        String key = String.format(idf_dict_key, recommendInfo.projectName);
        if (cache.exists(key)) {
            Map<String, Object> map = cache.hGetAllObject(key, null);
            Map<String, Double> res = Maps.newHashMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                res.put(entry.getKey(), (Double) entry.getValue());
            }
            return res;
        } else {
            String sql = "select * from idf_dict";
            List<IDFDict> list = this.jdbcTemplate.query(sql, rowMapper);
            Map<String, Object> redisMap = Maps.newHashMap();
            Map<String, Double> res = Maps.newHashMap();
            for (IDFDict one : list) {
                redisMap.put(one.getWord(), one.getWeight());
                res.put(one.getWord(), one.getWeight());
            }
            if(redisMap.size() > 0) {
                cache.hMultiSetObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_overtime_long);
            }
            return res;
        }
    }

    @Override
    public boolean delete(long id) throws DataAccessException {
        String sql = "select * from idf_dict where id = %d";
        IDFDict dbIdfDict = this.jdbcTemplate.query(String.format(sql, id), resultSetExtractor);
        if (dbIdfDict != null) {
            sql = "delete from idf_dict where id = %d";
            sql = String.format(sql, id);
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(idf_dict_key, recommendInfo.projectName);
                cache.hDelete(key, dbIdfDict.getWord());
            }
            return res > 0;
        }
        return false;
    }

    @Override
    public boolean update(IDFDict idfDict) throws DataAccessException {
        String sql = "select * from idf_dict where id = %d";
        IDFDict dbIdfDict = this.jdbcTemplate.queryForObject(String.format(sql, idfDict.getId()), rowMapper);
        if (dbIdfDict != null) {
            sql = "update idf_dict set word = '%s', weight = %s where id = %d";
            sql = String.format(sql, idfDict.getWord(), idfDict.getWeight(), idfDict.getId());
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(idf_dict_key, recommendInfo.projectName);
                cache.hDelete(key, dbIdfDict.getWord());
                cache.hSetObject(key, idfDict.getWord(), idfDict.getWeight());
            }
            return res > 0;
        }
        return false;
    }

    @Override
    public int insert(List<IDFDict> idfDicts) throws DataAccessException {
        int res = 0;
        String sqlPrev = "insert ignore into idf_dict (word, weight) values ";
        String sqlValueFormat = "('%s', %s)";
        List<String> sqlValueList = Lists.newArrayList();
        for (IDFDict idfDict : idfDicts) {
            String sqlValue = String.format(sqlValueFormat, idfDict.getWord(), idfDict.getWeight());
            sqlValueList.add(sqlValue);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues;
        long startTime = System.currentTimeMillis();
        res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        String key = String.format(idf_dict_key, recommendInfo.projectName);
        if (res == idfDicts.size()) {
            if (cache.exists(key)) {
                Map<String, Object> redisMap = Maps.newHashMap();
                for (IDFDict idfDict : idfDicts) {
                    redisMap.put(idfDict.getWord(), idfDict.getWeight());
                }
                cache.hMultiSetObject(key, redisMap);
            }
        } else {
            cache.delete(key);
        }

        return res;
    }

}
