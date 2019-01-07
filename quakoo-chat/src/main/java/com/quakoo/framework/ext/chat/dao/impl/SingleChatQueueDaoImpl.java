package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.SingleChatQueueDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;

public class SingleChatQueueDaoImpl extends BaseDaoHandle implements SingleChatQueueDao {

	private final static String single_chat_queue_status_key = "%s_single_chat_queue_%s_status_%d";
	private final static String single_chat_queue_status_null_key = "%s_single_chat_queue_%s_status_%d_null";
	private final static String single_chat_object_key = "%s_single_chat_object_uid_%d_toUid_%d_mid_%d";

    private Logger logger = LoggerFactory.getLogger(SingleChatQueueDaoImpl.class);

    private String getTable(long uid){
		int index = (int) uid % chatInfo.single_chat_queue_table_names.size();
		return chatInfo.single_chat_queue_table_names.get(index);
	}

	@Override
	public boolean insert(SingleChatQueue one) throws DataAccessException {
		boolean res = false;
		long uid = one.getUid();
		long toUid = one.getToUid();
		long mid = one.getMid();
		String tableName = this.getTable(one.getUid());
		int status = one.getStatus();
		long time = one.getTime();
		String sql = "insert ignore into %s (uid, toUid, mid, status, time) values (?, ?, ?, ?, ?)";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, uid, toUid, mid, status, time);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        res = ret > 0 ? true : false;
		
		if(res){
			String queue_key = String.format(single_chat_queue_status_key, chatInfo.projectName, tableName, status);
			String queue_null_key = String.format(single_chat_queue_status_null_key, chatInfo.projectName, 
					tableName, status);
			String object_key = String.format(single_chat_object_key, chatInfo.projectName, uid, toUid, mid);
			cache.delete(queue_null_key);
			cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
			if(cache.exists(queue_key)){
				cache.zaddObject(queue_key, new Double(time), one);
			}
		}
		return res;
	}

    @Override
	public boolean exist(SingleChatQueue one) throws DataAccessException {
		long uid = one.getUid();
		long toUid = one.getToUid();
		long mid = one.getMid();
		String object_key = String.format(single_chat_object_key, chatInfo.projectName, uid, toUid, mid);
		Object obj = cache.getObject(object_key, null);
		if(null != obj){
			return true;
		} else {
			String tableName = getTable(uid);
			String sql = "select * from %s where uid = %d and toUid = %d and mid = %d";
			sql = String.format(sql, tableName, uid, toUid, mid);
			long startTime = System.currentTimeMillis();
		    one = this.jdbcTemplate.query(sql, new SingleChatQueueResultSetExtractor());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

            if(null != one){
		    	cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
		    	return true;
		    } else
		    	return false;
		}
	}

//    @Override
//	public boolean delete(SingleChatQueue one) throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		long toUid = one.getToUid();
//		long mid = one.getMid();
//		int status =one.getStatus();
//		String tableName = getTable(uid);
//		String sql = "delete from %s where uid = ? and toUid = ? and mid = ?";
//		sql = String.format(sql, tableName);
//		long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, toUid, mid);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//        res = ret > 0 ? true : false;
//
//		if(res){
//			String object_key = String.format(single_chat_object_key, chatInfo.projectName, uid, toUid, mid);
//			cache.delete(object_key);
//			String queue_key = String.format(single_chat_queue_status_key, chatInfo.projectName, tableName, status);
//			if(cache.exists(queue_key)){
//				cache.zremObject(queue_key, one);
//			}
//		}
//		return res;
//	}

    @Override
	public boolean update(SingleChatQueue one, int newStatus)
			throws DataAccessException {
		boolean res = false;
		long uid = one.getUid();
		long toUid = one.getToUid();
		long mid = one.getMid();
		long time = one.getTime();
		int status = one.getStatus();
		String tableName = getTable(uid);
		String sql = "update %s set status = ? where uid = ? and toUid = ? and mid = ?";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, newStatus, uid, toUid, mid);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        res = ret > 0 ? true : false;
		
		if(res){
			String queue_key = String.format(single_chat_queue_status_key, chatInfo.projectName, tableName, status);
			if(cache.exists(queue_key)) 
				cache.zremObject(queue_key, one);
			String queue_new_null_key = String.format(single_chat_queue_status_null_key, chatInfo.projectName,  
					tableName, newStatus);
			cache.delete(queue_new_null_key);
			String queue_new_key = String.format(single_chat_queue_status_key, chatInfo.projectName, 
					tableName, newStatus);
			one.setStatus(newStatus);
			String object_key = String.format(single_chat_object_key, chatInfo.projectName, uid, toUid, mid);
			cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
			if(cache.exists(queue_new_key))
				cache.zaddObject(queue_new_key, new Double(time), one);
		}
		return res;
	}
	
	private void init(String table_name, int status){
		String queue_key = String.format(single_chat_queue_status_key, chatInfo.projectName, table_name, status);
		String queue_null_key = String.format(single_chat_queue_status_null_key, chatInfo.projectName, 
				table_name, status);
		if(!cache.exists(queue_null_key) && !cache.exists(queue_key)){
			ZkLock lock = null;
			try {
				lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
						chatInfo.projectName, queue_key + AbstractChatInfo.lock_suffix, 
						true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
				if(!cache.exists(queue_null_key) && !cache.exists(queue_key)){
					String sql = "select * from %s where status = %d order by time asc";
					sql = String.format(sql, table_name, status);
					long startTime = System.currentTimeMillis();
					List<SingleChatQueue> all_list = this.jdbcTemplate.query(sql, 
							new SingleChatQueueRowMapper());
                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

                    if(null != all_list && all_list.size() > 0){
						Map<Object, Double> map = Maps.newHashMap();
				    	for(SingleChatQueue one : all_list){
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

    @Override
	public List<SingleChatQueue> all_list(String table_name, int status,
			int size) throws DataAccessException {
		this.init(table_name, status);
		String queue_key = String.format(single_chat_queue_status_key, chatInfo.projectName, table_name, status);
		String queue_null_key = String.format(single_chat_queue_status_null_key, chatInfo.projectName, 
				table_name, status);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			List<SingleChatQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((SingleChatQueue) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}

//    @Override
//	public List<SingleChatQueue> list_time(String table_name, int status,
//			long maxTime, int size) throws DataAccessException {
//		this.init(table_name, status);
//		String queue_key = String.format(single_chat_queue_status_key, chatInfo.projectName, table_name, status);
//		String queue_null_key = String.format(single_chat_queue_status_null_key, chatInfo.projectName,
//				table_name, status);
//		if(cache.exists(queue_null_key))
//			return Lists.newArrayList();
//		if(cache.exists(queue_key)) {
//			List<SingleChatQueue> res = Lists.newArrayList();
//			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, new Double(maxTime), 0, size, null);
//			if(null != set && set.size() > 0){
//			    for(Object obj : set){
//			   		res.add((SingleChatQueue) obj);
//			   	}
//			}
//			return res;
//		}
//		return Lists.newArrayList();
//	}

    @Override
	public boolean list_null(String table_name, int status) {
		String queue_null_key = String.format(single_chat_queue_status_null_key, chatInfo.projectName, 
				table_name, status);
		return cache.exists(queue_null_key);
	}
	
	class SingleChatQueueRowMapper implements RowMapper<SingleChatQueue> {
		@Override
		public SingleChatQueue mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			SingleChatQueue res = new SingleChatQueue();
			res.setMid(rs.getLong("mid"));
			res.setStatus(rs.getInt("status"));
			res.setTime(rs.getLong("time"));
			res.setToUid(rs.getLong("toUid"));
			res.setUid(rs.getLong("uid"));
			return res;
		}
	}
	
	class SingleChatQueueResultSetExtractor implements ResultSetExtractor<SingleChatQueue> {
		@Override
		public SingleChatQueue extractData(ResultSet rs) throws SQLException,
				DataAccessException {
			if (rs.next()) {
				SingleChatQueue res = new SingleChatQueue();
				res.setMid(rs.getLong("mid"));
				res.setStatus(rs.getInt("status"));
				res.setTime(rs.getLong("time"));
				res.setToUid(rs.getLong("toUid"));
				res.setUid(rs.getLong("uid"));
				return res;
			} else
				return null;
		}
	}

}
