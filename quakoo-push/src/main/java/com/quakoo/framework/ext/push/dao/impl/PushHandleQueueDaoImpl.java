//package com.quakoo.framework.ext.push.dao.impl;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import com.google.common.collect.Sets;
//import com.quakoo.baseFramework.jackson.JsonUtils;
//import com.quakoo.baseFramework.redis.RedisSortData;
//import com.quakoo.framework.ext.push.model.Payload;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.dao.DataAccessException;
//import org.springframework.jdbc.core.BatchPreparedStatementSetter;
//import org.springframework.jdbc.core.PreparedStatementCreator;
//import org.springframework.jdbc.core.RowMapper;
//import org.springframework.jdbc.support.GeneratedKeyHolder;
//import org.springframework.jdbc.support.KeyHolder;
//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.quakoo.baseFramework.lock.ZkLock;
//import com.quakoo.framework.ext.push.dao.BaseDao;
//import com.quakoo.framework.ext.push.dao.PushHandleQueueDao;
//import com.quakoo.framework.ext.push.model.PushHandleQueue;
//
//public class PushHandleQueueDaoImpl extends BaseDao implements PushHandleQueueDao, InitializingBean {
//
//    Logger logger = LoggerFactory.getLogger(PushHandleQueueDaoImpl.class);
//
//	private String push_handle_queue_key;
//	private String push_handle_queue_null_key;
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        push_handle_queue_key = pushInfo.projectName + "_push_handle_queue_%s";
//        push_handle_queue_null_key = pushInfo.projectName + "_push_handle_queue_%s_null";
//    }
//
//    private String getTable(int shardNum){
//		int index = shardNum % pushInfo.push_handle_queue_table_names.size();
//		return pushInfo.push_handle_queue_table_names.get(index);
//	}
//
//    @Override
//    public void insert(List<PushHandleQueue> list) throws DataAccessException {
//        String sqlPrev = "insert ignore into %s (id, shardNum, type, uid, uids, `time`) values ";
//        String sqlFormat = "(%d, %d, %d, %d, '%s', %d)";
//        Map<String, List<PushHandleQueue>> maps = Maps.newLinkedHashMap();
//        for(PushHandleQueue one : list){
//            int shardNum = Math.abs((String.valueOf(one.getType()) +
//                    String.valueOf(one.getUid()) + one.getUids()).hashCode());
//            one.setShardNum(shardNum);
//            one.setTime(System.currentTimeMillis());
//            String tableName = getTable(one.getShardNum());
//            List<PushHandleQueue> subList = maps.get(tableName);
//            if(null == subList){
//                subList = Lists.newArrayList();
//                maps.put(tableName, subList);
//            }
//            subList.add(one);
//        }
//        List<String> sqls = Lists.newArrayList();
//        List<String> queue_null_key_list = Lists.newArrayList();
//        List<String> queue_key_list = Lists.newArrayList();
//        List<Map.Entry<String, List<PushHandleQueue>>> entries = Lists.newArrayList();
//        for(Map.Entry<String, List<PushHandleQueue>> entry : maps.entrySet()){
//            String tableName = entry.getKey();
//            List<PushHandleQueue> subList = entry.getValue();
//            List<String> sqlValueList = Lists.newArrayList();
//            for(PushHandleQueue one : subList){
//                String sqlValue = String.format(sqlFormat, one.getId(), one.getShardNum(), one.getType(),
//                        one.getUid(), one.getUids(), one.getTime());
//                sqlValueList.add(sqlValue);
//            }
//            String sqlValues = StringUtils.join(sqlValueList, ",");
//            String sql = String.format(sqlPrev, tableName) + sqlValues;
//            sqls.add(sql);
//            String queue_key = String.format(push_handle_queue_key, tableName);
//            queue_key_list.add(queue_key);
//            String queue_null_key = String.format(push_handle_queue_null_key, tableName);
//            queue_null_key_list.add(queue_null_key);
//            entries.add(entry);
//        }
//        long startTime = System.currentTimeMillis();
//        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
//        if(resList.length != entries.size()) {
//            cache.multiDelete(queue_key_list);
//            cache.multiDelete(queue_null_key_list);
//        } else {
//            for(int i = 0; i < entries.size(); i++) {
//                int success = resList[i];
//                Map.Entry<String, List<PushHandleQueue>> entry = entries.get(i);
//                String tableName = entry.getKey();
//                List<PushHandleQueue> subQueueList = entry.getValue();
//                int param_num = subQueueList.size();
//                String queue_key = String.format(push_handle_queue_key, tableName);
//                String queue_null_key = String.format(push_handle_queue_null_key, tableName);
//                if(success != param_num) {
//                    cache.multiDelete(Lists.newArrayList(queue_key, queue_null_key));
//                } else {
//                    cache.delete(queue_null_key);
//                    boolean exists = cache.exists(queue_key);
//                    if(exists) {
//                        Map<Object, Double> redisMap = Maps.newHashMap();
//                        for(PushHandleQueue one : subQueueList) {
//                            redisMap.put(one, new Double(one.getTime()));
//                        }
//                        cache.zaddMultiObject(queue_key, redisMap);
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//	public boolean insert(PushHandleQueue one) throws DataAccessException {
//		boolean res = false;
//		final int shardNum = Math.abs((String.valueOf(one.getType()) +
//                String.valueOf(one.getUid()) + one.getUids()).hashCode());
//		one.setShardNum(shardNum);
//		String tableName = this.getTable(shardNum);
////		final long payloadId = one.getPayloadId();
//		final long time = System.currentTimeMillis();
//		one.setTime(time);
//		final long id = one.getId();
//		final int type = one.getType();
//		final long uid = one.getUid();
//		final String uids = one.getUids();
//	    String sqlFormat = "insert ignore into %s (id, shardNum, type, uid, uids, time) values (?, ?, ?, ?, ?, ?)";
//		final String sql = String.format(sqlFormat, tableName);
//		PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
//			@Override
//			public PreparedStatement createPreparedStatement(Connection con)
//					throws SQLException {
//				PreparedStatement ps = con.prepareStatement(sql,
//                        new String[] { "id" });
//                ps.setLong(1, id);
//                ps.setInt(2, shardNum);
//                ps.setInt(3, type);
//                ps.setLong(4, uid);
//                ps.setString(5, uids);
//                ps.setLong(6, time);
//                return ps;
//			}
//		};
////		KeyHolder key = new GeneratedKeyHolder();
//        long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(preparedStatementCreator);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//		res = ret > 0 ? true : false;
//		if(res){
////			long id = key.getKey().longValue();
////			one.setId(id);
//			String queue_key = String.format(push_handle_queue_key, tableName);
//			String queue_null_key = String.format(push_handle_queue_null_key, tableName);
//			cache.delete(queue_null_key);
//			if(cache.exists(queue_key)){
//				cache.zaddObject(queue_key, new Double(time), one);
//			}
//		}
//		return res;
//	}
//
//    @Override
//    public void delete(List<PushHandleQueue> list) throws DataAccessException {
//        String sql = "delete from %s where id in (%s)";
//        Map<String, List<PushHandleQueue>> maps = Maps.newHashMap();
//        for(PushHandleQueue one : list){
//            String tableName = getTable(one.getShardNum());
//            List<PushHandleQueue> subList = maps.get(tableName);
//            if(null == subList){
//                subList = Lists.newArrayList();
//                maps.put(tableName, subList);
//            }
//            subList.add(one);
//        }
//        List<String> sqls = Lists.newArrayList();
//        List<List<PushHandleQueue>> queuesList = Lists.newArrayList();
//        for(Map.Entry<String, List<PushHandleQueue>> entry : maps.entrySet()){
//            String tableName = entry.getKey();
//            List<PushHandleQueue> subList = entry.getValue();
//            List<Long> ids = Lists.newArrayList();
//            for(PushHandleQueue one : subList){
//                ids.add(one.getId());
//            }
//            String subSql = String.format(sql, tableName, StringUtils.join(ids, ","));
//            sqls.add(subSql);
//            queuesList.add(subList);
//        }
//        long startTime = System.currentTimeMillis();
//        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
//        Set<String> queue_key_set = Sets.newHashSet();
//        for(Map.Entry<String, List<PushHandleQueue>> entry : maps.entrySet()) {
//            String queue_key = String.format(push_handle_queue_key, entry.getKey());
//            queue_key_set.add(queue_key);
//        }
//
//        if(resList.length != queuesList.size()) {
//            cache.multiDelete(Lists.newArrayList(queue_key_set));
//        }
//        for(int i = 0; i < resList.length; i++) {
//            int success = resList[i];
//            List<PushHandleQueue> sub_queues = queuesList.get(i);
//            int param_num = sub_queues.size();
//            String tableName = getTable(sub_queues.get(0).getShardNum());
//            String queue_key = String.format(push_handle_queue_key, tableName);
//            if(success != param_num){
//                cache.delete(queue_key);
//            } else {
//                if(cache.exists(queue_key)){
//                    List<Object> params = Lists.newArrayList();
//                    for(PushHandleQueue one : sub_queues) {
//                        params.add(one);
//                    }
//                    if(params.size() > 0) cache.zremMultiObject(queue_key, params);
//                }
//            }
//        }
//    }
//
//    @Override
//	public boolean delete(PushHandleQueue one) throws DataAccessException {
//		boolean res = false;
//		int shardNum = one.getShardNum();
//		long id = one.getId();
//		String tableName = getTable(shardNum);
//		String sql = "delete from %s where id = ?";
//		sql = String.format(sql, tableName);
//        long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, id);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//		res = ret > 0 ? true : false;
//		if(res){
//			String queue_key = String.format(push_handle_queue_key, tableName);
//			if(cache.exists(queue_key)){
//				cache.zremObject(queue_key, one);
//			}
//		}
//		return res;
//	}
//
//	private void init(String table_name) {
//		String queue_key = String.format(push_handle_queue_key, table_name);
//		String queue_null_key = String.format(push_handle_queue_null_key, table_name);
//		if(!cache.exists(queue_null_key) && !cache.exists(queue_key)){
//			ZkLock lock = null;
//			try {
//				lock = ZkLock.getAndLock(pushInfo.pushLockZkAddress,
//						pushInfo.projectName, queue_key + pushInfo.lock_suffix,
//						true, pushInfo.session_timout, pushInfo.lock_timeout);
//				if(!cache.exists(queue_null_key) && !cache.exists(queue_key)) {
//					String sql = "select * from %s order by time asc";
//					sql = String.format(sql, table_name);
//                    long startTime = System.currentTimeMillis();
//					List<PushHandleQueue> all_list = this.jdbcTemplate.query(sql,
//							new PushHandleQueueRowMapper());
//                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//					if(null != all_list && all_list.size() > 0){
//						Map<Object, Double> map = Maps.newHashMap();
//				    	for(PushHandleQueue one : all_list){
//				    		Double score = new Double(one.getTime());
//				    		map.put(one, score);
//				    	}
//				    	if(map.size() > 0){
//				    		cache.zaddMultiObject(queue_key, map);
//				    		cache.expire(queue_key, pushInfo.redis_overtime_long);
//				    	}
//					}else{
//						cache.setString(queue_null_key, pushInfo.redis_overtime_long, "true");
//					}
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (lock != null) lock.release();
//			}
//		}
//	}
//
//	@Override
//	public List<PushHandleQueue> list(String table_name, int size)
//			throws DataAccessException {
//		this.init(table_name);
//		String queue_key = String.format(push_handle_queue_key, table_name);
//		String queue_null_key = String.format(push_handle_queue_null_key, table_name);
//		if(cache.exists(queue_null_key))
//			return Lists.newArrayList();
//		if(cache.exists(queue_key)){
//			List<PushHandleQueue> res = Lists.newArrayList();
//			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
//			if(null != set && set.size() > 0){
//			    for(Object one : set){
//			   		res.add((PushHandleQueue) one);
//			   	}
//			}
//			return res;
//		}
//		return Lists.newArrayList();
//	}
//
//	class PushHandleQueueRowMapper implements RowMapper<PushHandleQueue> {
//		@Override
//		public PushHandleQueue mapRow(ResultSet rs, int rowNum)
//				throws SQLException {
//			PushHandleQueue res = new PushHandleQueue();
//			res.setId(rs.getLong("id"));
////			res.setPayloadId(rs.getLong("payloadId"));
//			res.setShardNum(rs.getInt("shardNum"));
//			res.setTime(rs.getLong("time"));
//			res.setType(rs.getInt("type"));
//			res.setUid(rs.getLong("uid"));
//			res.setUids(rs.getString("uids"));
//			return res;
//		}
//	}
//
//}
