package com.quakoo.framework.ext.push.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;

import com.quakoo.framework.ext.push.model.PushUserQueue;

public class PushUserQueueDaoImpl extends BaseDao implements PushUserQueueDao {

    private Logger logger = LoggerFactory.getLogger(PushUserQueueDaoImpl.class);

    private String getTable(long uid){
        int index = (int) uid % pushInfo.push_user_queue_table_names.size();
        return pushInfo.push_user_queue_table_names.get(index);
    }

    @Override
    public void insert(List<Long> uids) throws DataAccessException {
        String sqlPrev = "insert ignore into %s (uid) values ";
        String sqlValueFormat = "(%d)";
        Map<String, List<Long>> maps = Maps.newHashMap();
        for (long uid : uids) {
            String tableName = getTable(uid);
            List<Long> list = maps.get(tableName);
            if (null == list) {
                list = Lists.newArrayList();
                maps.put(tableName, list);
            }
            list.add(uid);
        }
        List<String> sqls = Lists.newArrayList();
        for (Map.Entry<String, List<Long>> entry : maps.entrySet()) {
            String tableName = entry.getKey();
            List<Long> list = entry.getValue();
            List<String> sqlValueList = Lists.newArrayList();
            for(long one : list){
                String sqlValue = String.format(sqlValueFormat, one);
                sqlValueList.add(sqlValue);
            }
            String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
            String sql = sqlPrev + sqlValues;
            sql = String.format(sql, tableName);
            sqls.add(sql);
        }
        long startTime = System.currentTimeMillis();
        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
    }

    @Override
    public void delete(List<Long> uids) throws DataAccessException {
        String sqlFormat = "delete from %s where uid in (%s)";
        Map<String, List<Long>> maps = Maps.newHashMap();
        for (long uid : uids) {
            String tableName = getTable(uid);
            List<Long> list = maps.get(tableName);
            if (null == list) {
                list = Lists.newArrayList();
                maps.put(tableName, list);
            }
            list.add(uid);
        }
        List<String> sqls = Lists.newArrayList();
        for (Map.Entry<String, List<Long>> entry : maps.entrySet()) {
            String tableName = entry.getKey();
            List<Long> list = entry.getValue();
            String param = StringUtils.join(list, ",");
            String sql = String.format(sqlFormat, tableName, param);
            sqls.add(sql);
        }
        long startTime = System.currentTimeMillis();
        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
    }

    //    @Override
//	public void insert(PushUserQueue one) throws DataAccessException {
//		long uid = one.getUid();
//		String tableName = this.getTable(uid);
//		String sql = "insert ignore into %s (uid) values (?)";
//		sql = String.format(sql, tableName);
//        long startTime = System.currentTimeMillis();
//		this.jdbcTemplate.update(sql, uid);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//	}

    @Override
    public List<PushUserQueue> getList(String table_name, long index, int size)
            throws DataAccessException {
        String sql = "select * from %s where `index` > %d order by `index` asc limit %d";
        sql = String.format(sql, table_name, index, size);
        long startTime = System.currentTimeMillis();
        List<PushUserQueue> list = this.jdbcTemplate.query(sql,
                new PushUserQueueRowMapper());
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        return list;
    }

    class PushUserQueueRowMapper implements RowMapper<PushUserQueue> {
        @Override
        public PushUserQueue mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            PushUserQueue res = new PushUserQueue();
            res.setIndex(rs.getLong("index"));
            res.setUid(rs.getLong("uid"));
            return res;
        }
    }

}
