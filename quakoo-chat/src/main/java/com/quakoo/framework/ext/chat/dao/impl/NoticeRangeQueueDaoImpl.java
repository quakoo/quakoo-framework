package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.NoticeRangeQueueDao;
import com.quakoo.framework.ext.chat.model.NoticeRangeQueue;

public class NoticeRangeQueueDaoImpl extends BaseDaoHandle implements NoticeRangeQueueDao {
	
	private final static String notice_range_queue_status_key = "%s_notice_range_queue_status_%d";
	private final static String notice_range_queue_status_null_key = "%s_notice_range_queue_status_%d_null";

    private Logger logger = LoggerFactory.getLogger(NoticeRangeQueueDaoImpl.class);


    public boolean insert(NoticeRangeQueue one) throws DataAccessException {
		boolean res = false;
		long authorId = one.getAuthorId();
		long mid = one.getMid();
		String uids = one.getUids();
		int status = one.getStatus();
		long time = one.getTime();
		String sql = "insert ignore into notice_range_queue (authorId, mid, uids, status, time) values (?, ?, ?, ?, ?)";

		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, authorId, mid, uids, status, time);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
		res = ret > 0 ? true : false;
		
		if(res){
			String queue_key = String.format(notice_range_queue_status_key, chatInfo.projectName, status);
			String queue_null_key = String.format(notice_range_queue_status_null_key, chatInfo.projectName, status);
			cache.delete(queue_null_key);
			if(cache.exists(queue_key)){
				cache.zaddObject(queue_key, new Double(time), one);
			}
		}
		return res;
	}

	public boolean update(NoticeRangeQueue one, int newStatus)
			throws DataAccessException {
		boolean res = false;
		long authorId = one.getAuthorId();
		long mid = one.getMid();
		long time = one.getTime();
		int status = one.getStatus();
		String sql = "update notice_range_queue set status = ? where authorId = ? and mid = ?";
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, newStatus, authorId, mid);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
		res = ret > 0 ? true : false;
		
		if(res){
			String queue_key = String.format(notice_range_queue_status_key, chatInfo.projectName, status);
			if(cache.exists(queue_key))
				cache.zremObject(queue_key, one);
			String queue_new_null_key = String.format(notice_range_queue_status_null_key, chatInfo.projectName, newStatus);
			cache.delete(queue_new_null_key);
			String queue_new_key = String.format(notice_range_queue_status_key, chatInfo.projectName, newStatus);
			if(cache.exists(queue_new_key)){
				one.setStatus(newStatus);
				cache.zaddObject(queue_new_key, new Double(time), one);
			}
		}
		return res;
	}

	private void init(int status) {
		String queue_key = String.format(notice_range_queue_status_key, chatInfo.projectName, status);
		String queue_null_key = String.format(notice_range_queue_status_null_key, chatInfo.projectName, status);
		if(!cache.exists(queue_null_key) && !cache.exists(queue_key)){
			ZkLock lock = null;
			try {
				lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
						chatInfo.projectName, queue_key + AbstractChatInfo.lock_suffix, 
						true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
				if(!cache.exists(queue_null_key) && !cache.exists(queue_key)) {
					String sql = "select * from notice_range_queue where status = %d order by time asc";
					sql = String.format(sql, status);
					long startTime = System.currentTimeMillis();
					List<NoticeRangeQueue> all_list = this.jdbcTemplate.query(sql, new NoticeRangeQueueRowMapper());
                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
					if(null != all_list && all_list.size() > 0){
						Map<Object, Double> map = Maps.newHashMap();
				    	for(NoticeRangeQueue one : all_list){
				    		Double score = new Double(one.getTime());
				    		map.put(one, score);
				    	}
				    	if(map.size() > 0){
				    		cache.zaddMultiObject(queue_key, map);
				    		cache.expire(queue_key, AbstractChatInfo.redis_overtime_long);
				    	}
					}else{
						cache.setString(queue_null_key, AbstractChatInfo.redis_overtime_long, "true");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (lock != null) lock.release();
			}
		}
	}
	
	public List<NoticeRangeQueue> all_list(int status, int size)
			throws DataAccessException {
		this.init(status);
		String queue_key = String.format(notice_range_queue_status_key, chatInfo.projectName, status);
		String queue_null_key = String.format(notice_range_queue_status_null_key, chatInfo.projectName, status);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			List<NoticeRangeQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((NoticeRangeQueue) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}

	public List<NoticeRangeQueue> list_time(int status, long maxTime, int size)
			throws DataAccessException {
		this.init(status);
		String queue_key = String.format(notice_range_queue_status_key, chatInfo.projectName, status);
		String queue_null_key = String.format(notice_range_queue_status_null_key, chatInfo.projectName, status);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			List<NoticeRangeQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, new Double(maxTime), 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object obj : set){
			   		res.add((NoticeRangeQueue) obj);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}

	public boolean list_null(int status) {
		String queue_null_key = String.format(notice_range_queue_status_null_key, chatInfo.projectName, status);
		return cache.exists(queue_null_key);
	}
	
	class NoticeRangeQueueRowMapper implements RowMapper<NoticeRangeQueue> {
		@Override
		public NoticeRangeQueue mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			NoticeRangeQueue res = new NoticeRangeQueue();
			res.setAuthorId(rs.getLong("authorId"));
			res.setMid(rs.getLong("mid"));
			res.setStatus(rs.getInt("status"));
			res.setTime(rs.getLong("time"));
			res.setUids(rs.getString("uids"));
			return res;
		}
	}
	
	class NoticeRangeQueueResultSetExtractor implements ResultSetExtractor<NoticeRangeQueue>{
		@Override
		public NoticeRangeQueue extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if(rs.next()){
				NoticeRangeQueue res = new NoticeRangeQueue();
				res.setAuthorId(rs.getLong("authorId"));
				res.setMid(rs.getLong("mid"));
				res.setStatus(rs.getInt("status"));
				res.setTime(rs.getLong("time"));
				res.setUids(rs.getString("uids"));
				return res;
			} else 
				return null;
		}
	}
}
