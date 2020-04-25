package com.quakoo.framework.ext.recommend.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.ESField;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import com.quakoo.framework.ext.recommend.dao.BaseDao;
import com.quakoo.framework.ext.recommend.dao.SyncInfoDao;
import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import com.quakoo.framework.ext.recommend.model.SyncInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncInfoDaoImpl extends BaseDao implements SyncInfoDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(StopWordDaoImpl.class);

    private JedisX cache;

    private final static String sync_info_key = "%s_sync_info";
    private final static String sync_info_null_key = "%s_sync_info_null";

    private ResultSetExtractor<SyncInfo> resultSetExtractor = new ResultSetExtractor<SyncInfo>() {
        @Override
        public SyncInfo extractData(ResultSet rs) throws SQLException, DataAccessException {
            if(rs.next()){
                SyncInfo syncInfo = new SyncInfo();
                syncInfo.setId(rs.getLong("id"));
                syncInfo.setSql(rs.getString("sql"));
                syncInfo.setEsIndex(rs.getString("esIndex"));
                String esFieldsStr = rs.getString("esFields");
                List<ESField> esFields = JsonUtils.fromJson(esFieldsStr, new TypeReference<List<ESField>>() {});
                syncInfo.setEsFields(esFields);
                syncInfo.setTrackingColumn(rs.getString("trackingColumn"));
                syncInfo.setBatchSize(rs.getInt("batchSize"));
                syncInfo.setEsId(rs.getString("esId"));
                syncInfo.setLastTrackingValue(rs.getLong("lastTrackingValue"));
                return syncInfo;
            } else
                return null;
        }
    };

    private RowMapper<SyncInfo> rowMapper = new RowMapper<SyncInfo>() {
        @Override
        public SyncInfo mapRow(ResultSet rs, int i) throws SQLException {
            SyncInfo syncInfo = new SyncInfo();
            syncInfo.setId(rs.getLong("id"));
            syncInfo.setSql(rs.getString("sql"));
            syncInfo.setEsIndex(rs.getString("esIndex"));
            String esFieldsStr = rs.getString("esFields");
            List<ESField> esFields = JsonUtils.fromJson(esFieldsStr, new TypeReference<List<ESField>>() {});
            syncInfo.setEsFields(esFields);
            syncInfo.setTrackingColumn(rs.getString("trackingColumn"));
            syncInfo.setBatchSize(rs.getInt("batchSize"));
            syncInfo.setEsId(rs.getString("esId"));
            syncInfo.setLastTrackingValue(rs.getLong("lastTrackingValue"));
            return syncInfo;
        }
    };

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public List<Map<String, Object>> syncList(SyncInfo syncInfo) throws DataAccessException {
        String sql = syncInfo.getSql();
        String trackingColumn = syncInfo.getTrackingColumn();
        long trackingValue = syncInfo.getLastTrackingValue();
        int size = syncInfo.getBatchSize();
        sql += " and %s > %d order by %s asc limit 0, %d";
        sql = String.format(sql, trackingColumn, trackingValue, trackingColumn, size + 1);
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> res = this.jdbcTemplate.queryForList(sql);
        logger.info("===== sql : " + sql + ", time : " + (System.currentTimeMillis() - startTime) + ", size : " + res.size());
        if(res.size() == size + 1) {
            long lastValue = (long)res.get(size - 1).get(trackingColumn);
            long nextValue = (long)res.get(size).get(trackingColumn);
            if(lastValue == nextValue) {
                sql = syncInfo.getSql() + " and %s > %d and %s <= %d order by %s asc";
                sql = String.format(sql, trackingColumn, trackingValue, trackingColumn, nextValue, trackingColumn);
                startTime = System.currentTimeMillis();
                res = this.jdbcTemplate.queryForList(sql);
                logger.info("===== sql : " + sql + ", time : " + (System.currentTimeMillis() - startTime) + ", size : " + res.size());
            } else {
                res = res.subList(0, size);
            }
        }
        return res;
    }

    @Override
    public List<SyncInfo> getSyncInfos() throws DataAccessException {
        String key = String.format(sync_info_key, recommendInfo.projectName);
        String null_key = String.format(sync_info_null_key, recommendInfo.projectName);
        boolean exists = cache.exists(key);
        boolean null_exists = cache.exists(null_key);
        if (exists || null_exists) {
            if(exists) {
                Set<Object> set = cache.zrangeByScoreObject(key, 0, Double.MAX_VALUE, null);
                List<SyncInfo> res = Lists.newArrayList();
                for(Object obj : set) {
                    res.add((SyncInfo) obj);
                }
                return res;
            } else {
                return Lists.newArrayList();
            }
        } else {
            String sql = "select * from sync_info";
            List<SyncInfo> res = this.jdbcTemplate.query(sql, rowMapper);
            if(res.size() > 0) {
                Map<Object, Double> redisMap = Maps.newHashMap();
                for(SyncInfo one : res) {
                    redisMap.put(one, (double)one.getId());
                }
                cache.zaddMultiObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_overtime_long);
            } else {
                cache.setString(null_key, AbstractRecommendInfo.redis_overtime_long, "y");
            }
            return res;
        }
    }

    @Override
    public boolean insert(SyncInfo syncInfo) throws DataAccessException {
        String sql = "insert into sync_info (`sql`, esIndex, esFields, trackingColumn, batchSize, esId, lastTrackingValue) values "
                + "(?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int[] types = new int[]{ Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER};
        PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sql, types);
        pscf.setReturnGeneratedKeys(true);
        PreparedStatementCreator psc = pscf.newPreparedStatementCreator(new Object[] {syncInfo.getSql(), syncInfo.getEsIndex(),
                JsonUtils.toJson(syncInfo.getEsFields()), syncInfo.getTrackingColumn(), syncInfo.getBatchSize(), syncInfo.getEsId(),
                syncInfo.getLastTrackingValue()});
        int ret = this.jdbcTemplate.update(psc, keyHolder);
        if(ret > 0) {
            long id = keyHolder.getKey().longValue();
            syncInfo.setId(id);
            String null_key = String.format(sync_info_null_key, recommendInfo.projectName);
            cache.delete(null_key);
            String key = String.format(sync_info_key, recommendInfo.projectName);
            if(cache.exists(key)) cache.zaddObject(key, (double)id, syncInfo);
        }
        return ret > 0;
    }

    @Override
    public boolean updateTrackingValue(long id, long trackingValue) throws DataAccessException {
        String sql = "select * from sync_info where id = %d";
        SyncInfo dbSyncInfo = this.jdbcTemplate.query(String.format(sql, id), resultSetExtractor);
        if(dbSyncInfo != null) {
            sql = "update sync_info set lastTrackingValue = %d where id = %d";
            sql = String.format(sql, trackingValue, id);
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(sync_info_key, recommendInfo.projectName);
                if(cache.exists(key)) {
                    cache.zremObject(key, dbSyncInfo);
                    dbSyncInfo.setLastTrackingValue(trackingValue);
                    cache.zaddObject(key, (double)id, dbSyncInfo);
                }
            }
            return res > 0;
        }
        return false;
    }

    @Override
    public boolean delete(long id) throws DataAccessException {
        String sql = "select * from sync_info where id = %d";
        SyncInfo dbSyncInfo = this.jdbcTemplate.query(String.format(sql, id), resultSetExtractor);
        if (dbSyncInfo != null) {
            sql = "delete from sync_info where id = %d";
            sql = String.format(sql, id);
            long startTime = System.currentTimeMillis();
            int res = this.jdbcTemplate.update(sql);
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if (res > 0) {
                String key = String.format(sync_info_key, recommendInfo.projectName);
                cache.zremObject(key, dbSyncInfo);
            }
            return res > 0;
        }
        return false;
    }

}
