package com.quakoo.framework.ext.recommend.dao.impl;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.dao.BaseDao;
import com.quakoo.framework.ext.recommend.dao.IDFMissWordDao;
import com.quakoo.framework.ext.recommend.model.IDFMissWord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class IDFMissWordDaoImpl extends BaseDao implements IDFMissWordDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(IDFMissWordDaoImpl.class);

    private final static String idf_miss_word_queue_key = "%s_idf_miss_word_queue";

    private JedisX cache;

    private RowMapper<IDFMissWord> rowMapper = new RowMapper<IDFMissWord>() {
        @Override
        public IDFMissWord mapRow(ResultSet rs, int i) throws SQLException {
            IDFMissWord idfMissWord = new IDFMissWord();
            idfMissWord.setId(rs.getLong("id"));
            idfMissWord.setWord(rs.getString("word"));
            idfMissWord.setNum(rs.getInt("num"));
            return idfMissWord;
        }
    };

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public void record(Set<String> words) throws DataAccessException {
        String key = String.format(idf_miss_word_queue_key, recommendInfo.projectName);
        cache.piprpushString(key, words);
    }

    @Override
    public List<String> getRecords(int size) throws DataAccessException {
        if(size <= 0) size = 1;
        String key = String.format(idf_miss_word_queue_key, recommendInfo.projectName);
        List<String> res = cache.lrangeAndDelString(key, 0, size - 1);
        return res;
    }

    @Override
    public List<IDFMissWord> load(List<String> words) throws DataAccessException {
        String sql = "select * from idf_miss_word where word in (%s)";
        List<String> params = Lists.newArrayList();
        for(String one : words) {
            params.add("?");
        }
        String param = StringUtils.join(params, ",");
        sql = String.format(sql, param);
        List<IDFMissWord> res = this.jdbcTemplate.query(sql, rowMapper, words.toArray());
        return res;
    }

    @Override
    public int insert(List<IDFMissWord> list) throws DataAccessException {
        int res = 0;
        String sqlPrev = "insert ignore into idf_miss_word (word, num) values ";
        String sqlValueFormat = "('%s', %d)";
        List<String> sqlValueList = Lists.newArrayList();
        for (IDFMissWord one : list) {
            String sqlValue = String.format(sqlValueFormat, one.getWord(), one.getNum());
            sqlValueList.add(sqlValue);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues;
        long startTime = System.currentTimeMillis();
        res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        return res;
    }

    @Override
    public void replaceNum(List<IDFMissWord> list) throws DataAccessException {
        String sqlPrev = "insert into idf_miss_word (id, num) values ";
        String sqlValueFormat = "(%d, %d)";
        String sqlSuffix = " on duplicate key update num = values(num)";
        List<String> sqlValueList = Lists.newArrayList();
        for (IDFMissWord one : list) {
            String sqlValue = String.format(sqlValueFormat, one.getId(), one.getNum());
            sqlValueList.add(sqlValue);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues + sqlSuffix;
        long startTime = System.currentTimeMillis();
        this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
    }

    @Override
    public void delete(long id) throws DataAccessException {
        String sql = "delete from idf_miss_word where id = ?";
        long startTime = System.currentTimeMillis();
        this.jdbcTemplate.update(sql, id);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws DataAccessException {
        String sql = "select * from idf_miss_word where 1 = 1 ";
        String sqlCount = "select count(id) from idf_miss_word where 1 = 1 ";
        if(StringUtils.isNotBlank(word)) {
            sql += " and word like '%" + word + "%' ";
            sqlCount += " and word like '%" + word + "%' ";
        }
        String limit = " limit %d, %d ";
        sql = sql + " order by `num` desc";
        sql = sql + String.format(limit, (pager.getPage() - 1) * pager.getSize(), pager.getSize());
        List<IDFMissWord> data = this.jdbcTemplate.query(sql, rowMapper);
        int count = this.jdbcTemplate.queryForObject(sqlCount, Integer.class);
        pager.setData(data);
        pager.setTotalCount(count);
        return pager;
    }

}
