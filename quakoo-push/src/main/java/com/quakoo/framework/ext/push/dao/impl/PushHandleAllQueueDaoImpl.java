package com.quakoo.framework.ext.push.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushHandleAllQueueDao;
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
import com.quakoo.framework.ext.push.model.PushHandleAllQueue;

public class PushHandleAllQueueDaoImpl extends BaseDao implements PushHandleAllQueueDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(PushHandleAllQueueDaoImpl.class);

	private String push_handle_all_queue_key;
	private String push_handle_all_queue_null_key;

    @Override
    public void afterPropertiesSet() throws Exception {
        push_handle_all_queue_key = pushInfo.projectName + "_push_handle_all_queue";
        push_handle_all_queue_null_key = pushInfo.projectName  + "_push_handle_all_queue_null";
    }

    @Override
	public boolean insert(PushHandleAllQueue one) throws DataAccessException {
        boolean res = false;
        final long payloadId = one.getPayloadId();
        final long time = System.currentTimeMillis();
        one.setTime(time);
        final String sql = "insert into push_handle_all_queue (payloadId, `time`) values (?, ?)";
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con)
                    throws SQLException {
                PreparedStatement ps = con.prepareStatement(sql,
                        new String[] { "id" });
                ps.setLong(1, payloadId);
                ps.setLong(2, time);
                return ps;
            }
        };
        KeyHolder key = new GeneratedKeyHolder();
        long startTime = System.currentTimeMillis();
        int ret = this.jdbcTemplate.update(preparedStatementCreator, key);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
        if(res) {
            long id = key.getKey().longValue();
            one.setId(id);
            cache.delete(push_handle_all_queue_null_key);
            if(cache.exists(push_handle_all_queue_key)){
                cache.zaddObject(push_handle_all_queue_key, new Double(id), one);
            }
        }
        return res;
	}
	
	private void init() {
        if(!cache.exists(push_handle_all_queue_null_key) && !cache.exists(push_handle_all_queue_key)) {
            ZkLock lock = null;
            try {
                lock = ZkLock.getAndLock(pushInfo.pushLockZkAddress,
                        pushInfo.projectName, push_handle_all_queue_key + pushInfo.lock_suffix,
                        true, pushInfo.session_timout, pushInfo.lock_timeout);
                if(!cache.exists(push_handle_all_queue_null_key) && !cache.exists(push_handle_all_queue_key)) {
                    String sql = "select * from push_handle_all_queue order by id asc";
                    long startTime = System.currentTimeMillis();
                    List<PushHandleAllQueue> all_list = this.jdbcTemplate.query(sql,
                            new PushHandleAllQueueRowMapper());
                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
                    if(null != all_list && all_list.size() > 0){
                        Map<Object, Double> map = Maps.newHashMap();
                        for(PushHandleAllQueue one : all_list){
                            Double score = new Double(one.getId());
                            map.put(one, score);
                        }
                        if(map.size() > 0){
                            cache.zaddMultiObject(push_handle_all_queue_key, map);
                            cache.expire(push_handle_all_queue_key, pushInfo.redis_overtime_long);
                        }
                    }else{
                        cache.setString(push_handle_all_queue_null_key, pushInfo.redis_overtime_long, "true");
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
	public List<PushHandleAllQueue> getList(long minId, int size)
			throws DataAccessException {
		this.init();
		if(cache.exists(push_handle_all_queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(push_handle_all_queue_key)){
			List<PushHandleAllQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(push_handle_all_queue_key, new Double(minId) + 0.1, 
					Double.MAX_VALUE, 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((PushHandleAllQueue) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}
	
	@Override
	public PushHandleAllQueue load(long id) throws DataAccessException {
		this.init();
		if(cache.exists(push_handle_all_queue_null_key)) {
			return null;
		}
		if(cache.exists(push_handle_all_queue_key)){
			Set<Object> set = cache.zrangeByScoreObject(push_handle_all_queue_key, new Double(id - 1) + 0.1, 
					Double.MAX_VALUE, 0, 1, null);
			if(null != set && set.size() > 0){
				return (PushHandleAllQueue) set.iterator().next() ;
			}
		}
		return null;
	}

	class PushHandleAllQueueRowMapper implements RowMapper<PushHandleAllQueue> {
		@Override
		public PushHandleAllQueue mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			PushHandleAllQueue res = new PushHandleAllQueue();
			res.setId(rs.getLong("id"));
			res.setPayloadId(rs.getLong("payloadId"));
			res.setTime(rs.getLong("time"));
			return res;
		}
	}

}
