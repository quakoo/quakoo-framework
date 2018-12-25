package com.quakoo.framework.ext.push.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushHandleQueueDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.framework.ext.push.model.PushHandleQueue;

public class PushHandleQueueDaoImpl extends BaseDao implements PushHandleQueueDao, InitializingBean {

    Logger logger = LoggerFactory.getLogger(PushHandleQueueDaoImpl.class);

	private String push_handle_queue_key;
	private String push_handle_queue_null_key;

    @Override
    public void afterPropertiesSet() throws Exception {
        push_handle_queue_key = pushInfo.projectName + "_push_handle_queue_%s";
        push_handle_queue_null_key = pushInfo.projectName + "_push_handle_queue_%s_null";
    }

    private String getTable(int shardNum){
		int index = shardNum % pushInfo.push_handle_queue_table_names.size();
		return pushInfo.push_handle_queue_table_names.get(index);
	}
	
	@Override
	public boolean insert(PushHandleQueue one) throws DataAccessException {
		boolean res = false;
		final int shardNum = Math.abs((String.valueOf(one.getType()) +
                String.valueOf(one.getUid()) + one.getUids()).hashCode());
		one.setShardNum(shardNum);
		String tableName = this.getTable(shardNum);
		final long payloadId = one.getPayloadId();
		final long time = System.currentTimeMillis();
		one.setTime(time);
		final int type = one.getType();
		final long uid = one.getUid();
		final String uids = one.getUids();
	    String sqlFormat = "insert ignore into %s (shardNum, type, uid, uids, payloadId, time) values (?, ?, ?, ?, ?, ?)";
		final String sql = String.format(sqlFormat, tableName);
		PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection con)
					throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql,  
                        new String[] { "id" });  
                ps.setInt(1, shardNum); 
                ps.setInt(2, type);
                ps.setLong(3, uid);
                ps.setString(4, uids);
                ps.setLong(5, payloadId);
                ps.setLong(6, time);
                return ps;
			}
		};
		KeyHolder key = new GeneratedKeyHolder();
		int ret = this.jdbcTemplate.update(preparedStatementCreator, key);
		res = ret > 0 ? true : false;
		if(res){
			long id = key.getKey().longValue();
			one.setId(id);
			String queue_key = String.format(push_handle_queue_key, tableName);
			String queue_null_key = String.format(push_handle_queue_null_key, tableName);
			cache.delete(queue_null_key);
			if(cache.exists(queue_key)){
				cache.zaddObject(queue_key, new Double(time), one);
			}
		}
		return res;
	}

	@Override
	public boolean delete(PushHandleQueue one) throws DataAccessException {
		boolean res = false;
		int shardNum = one.getShardNum();
		long id = one.getId();
		String tableName = getTable(shardNum);
		String sql = "delete from %s where id = ?";
		sql = String.format(sql, tableName);
		int ret = this.jdbcTemplate.update(sql, id);
		res = ret > 0 ? true : false;
		if(res){
			String queue_key = String.format(push_handle_queue_key, tableName);
			if(cache.exists(queue_key)){
				cache.zremObject(queue_key, one);
			}
		}
		return res;
	}
	
	private void init(String table_name) {
		String queue_key = String.format(push_handle_queue_key, table_name);
		String queue_null_key = String.format(push_handle_queue_null_key, table_name);
		if(!cache.exists(queue_null_key) && !cache.exists(queue_key)){
			ZkLock lock = null;
			try {
				lock = ZkLock.getAndLock(pushInfo.pushLockZkAddress,
						pushInfo.projectName, queue_key + pushInfo.lock_suffix,
						true, pushInfo.session_timout, pushInfo.lock_timeout);
				if(!cache.exists(queue_null_key) && !cache.exists(queue_key)) {
					String sql = "select * from %s order by time asc";
					sql = String.format(sql, table_name);
					List<PushHandleQueue> all_list = this.jdbcTemplate.query(sql, 
							new PushHandleQueueRowMapper());
					if(null != all_list && all_list.size() > 0){
						Map<Object, Double> map = Maps.newHashMap();
				    	for(PushHandleQueue one : all_list){
				    		Double score = new Double(one.getTime());
				    		map.put(one, score);
				    	}
				    	if(map.size() > 0){
				    		cache.zaddMultiObject(queue_key, map);
				    		cache.expire(queue_key, pushInfo.redis_overtime_long);
				    	}
					}else{
						cache.setString(queue_null_key, pushInfo.redis_overtime_long, "true");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (lock != null) lock.release();
			}
		}
	}
	
	@Override
	public List<PushHandleQueue> list(String table_name, int size)
			throws DataAccessException {
		this.init(table_name);
		String queue_key = String.format(push_handle_queue_key, table_name);
		String queue_null_key = String.format(push_handle_queue_null_key, table_name);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			List<PushHandleQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((PushHandleQueue) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}
	
	class PushHandleQueueRowMapper implements RowMapper<PushHandleQueue> {
		@Override
		public PushHandleQueue mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			PushHandleQueue res = new PushHandleQueue();
			res.setId(rs.getLong("id"));
			res.setPayloadId(rs.getLong("payloadId"));
			res.setShardNum(rs.getInt("shardNum"));
			res.setTime(rs.getLong("time"));
			res.setType(rs.getInt("type"));
			res.setUid(rs.getLong("uid"));
			res.setUids(rs.getString("uids"));
			return res;
		}
	}

}
