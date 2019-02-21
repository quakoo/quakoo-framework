package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.chat.model.SingleChatQueue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.ManyChatQueueDao;
import com.quakoo.framework.ext.chat.model.ManyChatQueue;

/**
 * 群聊消息队列DAO
 * class_name: ManyChatQueueDaoImpl
 * package: com.quakoo.framework.ext.chat.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:52
 **/
public class ManyChatQueueDaoImpl extends BaseDaoHandle implements ManyChatQueueDao, InitializingBean {

    private JedisX queueClient;

    private final static String many_chat_queue_key = "%s_many_chat_queue_%s";
//	private final static String many_chat_queue_status_key = "%s_many_chat_queue_%s_status_%d";
//	private final static String many_chat_queue_status_null_key = "%s_many_chat_queue_%s_status_%d_null";
	private final static String many_chat_object_key = "%s_many_chat_object_uid_%d_cgid_%d_mid_%d";

	private Logger logger = LoggerFactory.getLogger(ManyChatQueueDaoImpl.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        queueClient = new JedisX(chatInfo.queueInfo, chatInfo.queueConfig, 5000);
    }

    /**
     * 获取表名(根据UID获取表名)
	 * method_name: getTable
	 * params: [uid]
	 * return: java.lang.String
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:52
	 **/
//    private String getTable(long uid){
//		int index = (int) uid % chatInfo.many_chat_queue_table_names.size();
//		return chatInfo.many_chat_queue_table_names.get(index);
//	}
    private String getQueue(long uid) {
        int index = (int) uid % chatInfo.many_chat_queue_names.size();
        return chatInfo.many_chat_queue_names.get(index);
    }

	/**
     * 批量添加
	 * method_name: insert
	 * params: [queues]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.ManyChatQueue>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:53
	 **/
    @Override
    public List<ManyChatQueue> insert(List<ManyChatQueue> queues) throws DataAccessException {
        List<String> objectKeys = Lists.newArrayList();
        for (ManyChatQueue one : queues) {
            String object_key = String.format(many_chat_object_key, chatInfo.projectName, one.getUid(), one.getCgid(), one.getMid());
            objectKeys.add(object_key);
        }
        Map<String, Boolean> existsMap = queueClient.pipExists(objectKeys);
        for (Iterator<ManyChatQueue> it = queues.iterator(); it.hasNext(); ) {
            ManyChatQueue one = it.next();
            String object_key = String.format(many_chat_object_key, chatInfo.projectName, one.getUid(), one.getCgid(), one.getMid());
            Boolean exists = existsMap.get(object_key);
            if (null != exists && exists) it.remove();
        }
        Map<String, List<ManyChatQueue>> queuesMap = Maps.newHashMap();
        for (ManyChatQueue queue : queues) {
            String queueName = getQueue(queue.getUid());
            List<ManyChatQueue> list = queuesMap.get(queueName);
            if (null == list) {
                list = Lists.newArrayList();
                queuesMap.put(queueName, list);
            }
            list.add(queue);
        }
        List<ManyChatQueue> success = Lists.newArrayList();
        for (Map.Entry<String, List<ManyChatQueue>> entry : queuesMap.entrySet()) {
            String queueName = entry.getKey();
            List<ManyChatQueue> list = entry.getValue();
            String queue_key = String.format(many_chat_queue_key, chatInfo.projectName, queueName);
            Map<Object, Double> redisMap = Maps.newHashMap();
            for (ManyChatQueue one : list) {
                redisMap.put(one, new Double(one.getTime()));
            }
            Long sign = queueClient.zaddMultiObject(queue_key, redisMap);
            if(sign != null && sign == list.size()) success.addAll(list);
            logger.info("==== sign many chat queue insert : " + redisMap.size() + " ,res : " + sign + " " + list.toString());
        }
        if(success.size() > 0) {
            Map<String, Object> redisMap = Maps.newHashMap();
            for (ManyChatQueue one : success) {
                String object_key = String.format(many_chat_object_key, chatInfo.projectName, one.getUid(), one.getCgid(), one.getMid());
                redisMap.put(object_key, one);
            }
            queueClient.multiSetObject(redisMap, AbstractChatInfo.redis_overtime_long);
        }
        return success;
    }

//    @Override
//    public List<ManyChatQueue> insert(List<ManyChatQueue> queues) throws DataAccessException {
//        Map<String, List<ManyChatQueue>> tableQueuesMap = Maps.newHashMap();
//        for(ManyChatQueue queue : queues) {
//            String tableName = getTable(queue.getUid());
//            List<ManyChatQueue> list = tableQueuesMap.get(tableName);
//            if(null == list) {
//                list = Lists.newArrayList();
//                tableQueuesMap.put(tableName, list);
//            }
//            list.add(queue);
//        }
//        String sqlFormat = "insert ignore into %s (uid, cgid, mid, status, time) values (?, ?, ?, ?, ?)";
//        List<ManyChatQueue> successQueues = Lists.newArrayList();
//        for(Map.Entry<String, List<ManyChatQueue>> entry : tableQueuesMap.entrySet()) {
//            String tableName = entry.getKey();
//            final List<ManyChatQueue> list = entry.getValue();
//            String sql = String.format(sqlFormat, tableName);
//            long startTime = System.currentTimeMillis();
//            int[] res = this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {  //每个表批量处理
//                @Override
//                public void setValues(PreparedStatement ps, int i) throws SQLException {
//                    ManyChatQueue one =  list.get(i);
//                    ps.setLong(1, one.getUid());
//                    ps.setLong(2, one.getCgid());
//                    ps.setLong(3, one.getMid());
//                    ps.setInt(4, one.getStatus());
//                    ps.setLong(5, one.getTime());
//                }
//                @Override
//                public int getBatchSize() {
//                    return list.size();
//                }
//            });
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql
//                    + " , singleChatQueue : " + list.toString());
//            List<ManyChatQueue> oneSuccessQueues = Lists.newArrayList();
//            for(int i = 0; i < res.length; i++) {
//                int ret = res[i];
//                ManyChatQueue queue = list.get(i);
//                if(ret > 0) {  //持久化成功
//                    oneSuccessQueues.add(queue);
//                    successQueues.add(queue);
//                }
//            }
//            if(oneSuccessQueues.size() > 0) {
//                int status = oneSuccessQueues.get(0).getStatus();
//                String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, tableName, status);
//                String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//                        tableName, status);
//                cache.delete(queue_null_key);
//                Map<Object, Double> redisMap = Maps.newHashMap();
//                for(ManyChatQueue one : oneSuccessQueues) {
//                    redisMap.put(one, new Double(one.getTime()));
//                }
//                cache.zaddMultiObject(queue_key, redisMap);
//            }
//        }
//        if(successQueues.size() > 0) {
//            Map<String, Object> redisMap = Maps.newHashMap();
//            for(ManyChatQueue one : successQueues) {
//                String object_key = String.format(many_chat_object_key, chatInfo.projectName, one.getUid(), one.getCgid(), one.getMid());
//                redisMap.put(object_key, one);
//            }
//            cache.multiSetObject(redisMap, AbstractChatInfo.redis_overtime_long);
//        }
//        return successQueues;
//    }

    @Override
    public boolean insert(ManyChatQueue one) throws DataAccessException {
        String queueName = getQueue(one.getUid());
        String queue_key = String.format(many_chat_queue_key, chatInfo.projectName, queueName);
        queueClient.zaddObject(queue_key, new Double(one.getTime()), one);
        String object_key = String.format(many_chat_object_key, chatInfo.projectName, one.getUid(), one.getCgid(), one.getMid());
        queueClient.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
        return true;
    }
//    @Override
//	public boolean insert(ManyChatQueue one) throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		long cgid = one.getCgid();
//		long mid = one.getMid();
//		String tableName = this.getTable(one.getUid());
//		int status = one.getStatus();
//		long time = one.getTime();
//		String sql = "insert ignore into %s (uid, cgid, mid, status, time) values (?, ?, ?, ?, ?)";
//		sql = String.format(sql, tableName);
//		long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, cgid, mid, status, time);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//        res = ret > 0 ? true : false;
//
//		if(res){
//			String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, tableName, status);
//			String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//					tableName, status);
//			String object_key = String.format(many_chat_object_key, chatInfo.projectName, uid, cgid, mid);
//			cache.delete(queue_null_key);
//			cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
////			if(cache.exists(queue_key)){
//            cache.zaddObject(queue_key, new Double(time), one);
////			}
//		}
//		return res;
//	}

    @Override
    public boolean exist(ManyChatQueue one) throws DataAccessException {
        long uid = one.getUid();
        long cgid = one.getCgid();
        long mid = one.getMid();
        String object_key = String.format(many_chat_object_key, chatInfo.projectName, uid, cgid, mid);
        Object obj = queueClient.getObject(object_key, null);
        if (null != obj) {
            return true;
        } else {
            return false;
        }
    }
//    @Override
//	public boolean exist(ManyChatQueue one) throws DataAccessException {
//		long uid = one.getUid();
//		long cgid = one.getCgid();
//		long mid = one.getMid();
//		String object_key = String.format(many_chat_object_key, chatInfo.projectName, uid, cgid, mid);
//		Object obj = cache.getObject(object_key, null);
//		if(null != obj){
//			return true;
//		} else {
//			String tableName = getTable(uid);
//			String sql = "select * from %s where uid = %d and cgid = %d and mid = %d";
//			sql = String.format(sql, tableName, uid, cgid, mid);
//            long startTime = System.currentTimeMillis();
//		    one = this.jdbcTemplate.query(sql, new ManyChatQueueResultSetExtractor());
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//            if(null != one){
//		    	cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
//		    	return true;
//		    } else
//		    	return false;
//		}
//	}

    @Override
    public void delete(List<ManyChatQueue> queues) throws DataAccessException {
        Map<String, List<Object>> queuesMap = Maps.newHashMap();
        for (ManyChatQueue queue : queues) {
            String queueName = getQueue(queue.getUid());
            List<Object> list = queuesMap.get(queueName);
            if (null == list) {
                list = Lists.newArrayList();
                queuesMap.put(queueName, list);
            }
            list.add(queue);
        }
        for (Map.Entry<String, List<Object>> entry : queuesMap.entrySet()) {
            String queueName = entry.getKey();
            List<Object> list = entry.getValue();
            String queue_key = String.format(many_chat_queue_key, chatInfo.projectName, queueName);
            queueClient.zremMultiObject(queue_key, list);
        }
    }

//    @Override
//	public boolean delete(ManyChatQueue one) throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		long cgid = one.getCgid();
//		long mid = one.getMid();
//		int status =one.getStatus();
//		String tableName = getTable(uid);
//		String sql = "delete from %s where uid = ? and cgid = ? and mid = ?";
//		sql = String.format(sql, tableName);
//        long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, cgid, mid);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//        res = ret > 0 ? true : false;
//
//		if(res){
//			String object_key = String.format(many_chat_object_key, chatInfo.projectName, uid, cgid, mid);
//			cache.delete(object_key);
//			String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, tableName, status);
//			if(cache.exists(queue_key)){
//				cache.zremObject(queue_key, one);
//			}
//		}
//		return res;
//	}

    @Override
    public boolean delete(ManyChatQueue one) throws DataAccessException {
        long uid = one.getUid();
        String queueName = getQueue(uid);
        String queue_key = String.format(many_chat_queue_key, chatInfo.projectName, queueName);
        queueClient.zremObject(queue_key, one);
        return true;
    }

//    @Override
//    public void update(List<ManyChatQueue> queues, int newStatus) throws DataAccessException {
//        String sqlPrev = "update %s set status = %d where (uid, cgid, mid) in (%s)";
//        String sqlValueFormat = "(%d, %d, %d)";
//        Map<String, List<ManyChatQueue>> maps = Maps.newHashMap();
//        for(ManyChatQueue one : queues){
//            String tableName = getTable(one.getUid());
//            List<ManyChatQueue> list = maps.get(tableName);
//            if(null == list){
//                list = Lists.newArrayList();
//                maps.put(tableName, list);
//            }
//            list.add(one);
//        }
//        List<String> sqls = Lists.newArrayList();
//        List<List<ManyChatQueue>> queueList = Lists.newArrayList();
//        for(Map.Entry<String, List<ManyChatQueue>> entry : maps.entrySet()){
//            String tableName = entry.getKey();
//            List<ManyChatQueue> list = entry.getValue();
//            List<String> sqlValueList = Lists.newArrayList();
//            for(ManyChatQueue one : list){
//                String sqlValue = String.format(sqlValueFormat, one.getUid(), one.getCgid(), one.getMid());
//                sqlValueList.add(sqlValue);
//            }
//            String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
//            String sql = String.format(sqlPrev, tableName, newStatus, sqlValues);
//            sqls.add(sql);
//            queueList.add(list);
//        }
//        long startTime = System.currentTimeMillis();
//        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
//        for(int i = 0; i < resList.length; i++) {
//            int success = resList[i];
//            List<ManyChatQueue> sub_queues = queueList.get(i);
//            int param_num = sub_queues.size();
//            String tableName = getTable(sub_queues.get(0).getUid());
//            int oldStatus = sub_queues.get(0).getStatus();
//            String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, tableName, oldStatus);
//            String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName, tableName, oldStatus);
//            String queue_new_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName, tableName, newStatus);
//            String queue_new_key = String.format(many_chat_queue_status_key, chatInfo.projectName, tableName, newStatus);
//            if(param_num != success) {
//                cache.multiDelete(Lists.newArrayList(queue_key, queue_new_null_key, queue_new_key, queue_null_key));
//            } else {
//                if(cache.exists(queue_key)){
//                    List<Object> list = Lists.newArrayList();
//                    for(ManyChatQueue one : sub_queues) {
//                        list.add(one);
//                    }
//                    cache.zremMultiObject(queue_key, list);
//                }
//                if(!cache.exists(queue_key)) cache.setString(queue_null_key, AbstractChatInfo.redis_overtime_long, "true");
//                cache.delete(queue_new_null_key);
//                if(cache.exists(queue_new_key)) {
//                    Map<Object, Double> map = Maps.newHashMap();
//                    for(ManyChatQueue one : sub_queues) {
//                        one.setStatus(newStatus);
//                        map.put(one, new Double(one.getTime()));
//                    }
//                    cache.zaddMultiObject(queue_new_key, map);
//                }
//            }
//        }
//    }

//    @Override
//	public boolean update(ManyChatQueue one, int newStatus)
//			throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		long cgid = one.getCgid();
//		long mid = one.getMid();
//		long time = one.getTime();
//		int status = one.getStatus();
//		String tableName = getTable(uid);
//		String sql = "update %s set status = ? where uid = ? and cgid = ? and mid = ?";
//		sql = String.format(sql, tableName);
//		long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, newStatus, uid, cgid, mid);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//        res = ret > 0 ? true : false;
//
//		if(res){
//			String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, tableName, status);
//			if(cache.exists(queue_key))
//				cache.zremObject(queue_key, one);
//			String queue_new_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//					tableName, newStatus);
//			cache.delete(queue_new_null_key);
//			String queue_new_key = String.format(many_chat_queue_status_key, chatInfo.projectName,
//					tableName, newStatus);
//			one.setStatus(newStatus);
//			String object_key = String.format(many_chat_object_key, chatInfo.projectName, uid, cgid, mid);
//			cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, one);
//			if(cache.exists(queue_new_key))
//				cache.zaddObject(queue_new_key, new Double(time), one);
//		}
//		return res;
//	}
	
//	private void init(String table_name, int status) {
//		String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, table_name, status);
//		String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//				table_name, status);
//		if(!cache.exists(queue_null_key) && !cache.exists(queue_key)){
//			ZkLock lock = null;
//			try {
//				lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
//						chatInfo.projectName, queue_key + AbstractChatInfo.lock_suffix,
//						true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
//				if(!cache.exists(queue_null_key) && !cache.exists(queue_key)) {
//					String sql = "select * from %s where status = %d order by time asc";
//					sql = String.format(sql, table_name, status);
//					long startTime = System.currentTimeMillis();
//					List<ManyChatQueue> all_list = this.jdbcTemplate.query(sql,
//							new ManyChatQueueRowMapper());
//                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//                    if(null != all_list && all_list.size() > 0){
//						Map<Object, Double> map = Maps.newHashMap();
//				    	for(ManyChatQueue one : all_list){
//				    		Double score = new Double(one.getTime());
//				    		map.put(one, score);
//				    	}
//				    	if(map.size() > 0){
//				    		cache.zaddMultiObject(queue_key, map);
//				    		cache.expire(queue_key, AbstractChatInfo.redis_overtime_long);
//				    	}
//					}else{
//						cache.setString(queue_null_key, AbstractChatInfo.redis_overtime_long, "true");
//					}
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (lock != null) lock.release();
//			}
//		}
//	}

    @Override
    public List<ManyChatQueue> list(String queue_name, int size) throws DataAccessException {
        String queue_key = String.format(many_chat_queue_key, chatInfo.projectName, queue_name);
        List<ManyChatQueue> res = Lists.newArrayList();
        Set<Object> set = queueClient.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
        if (null != set && set.size() > 0) {
            for (Object one : set) {
                res.add((ManyChatQueue) one);
            }
        }
        return res;
    }

//	@Override
//	public List<ManyChatQueue> all_list(String table_name, int status, int size)
//			throws DataAccessException {
//		this.init(table_name, status);
//		String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, table_name, status);
//		String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//				table_name, status);
//		if(cache.exists(queue_key)){
//			List<ManyChatQueue> res = Lists.newArrayList();
//			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
//			if(null != set && set.size() > 0){
//			    for(Object one : set){
//			   		res.add((ManyChatQueue) one);
//			   	}
//			}
//			return res;
//		}
//        if(cache.exists(queue_null_key))
//            return Lists.newArrayList();
//		return Lists.newArrayList();
//	}

//	@Override
//	public List<ManyChatQueue> list_time(String table_name, int status,
//			long maxTime, int size) throws DataAccessException {
//		this.init(table_name, status);
//		String queue_key = String.format(many_chat_queue_status_key, chatInfo.projectName, table_name, status);
//		String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//				table_name, status);
//		if(cache.exists(queue_null_key))
//			return Lists.newArrayList();
//		if(cache.exists(queue_key)){
//			List<ManyChatQueue> res = Lists.newArrayList();
//			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, new Double(maxTime), 0, size, null);
//			if(null != set && set.size() > 0){
//			    for(Object obj : set){
//			   		res.add((ManyChatQueue) obj);
//			   	}
//			}
//			return res;
//		}
//		return Lists.newArrayList();
//	}

//	@Override
//	public boolean list_null(String table_name, int status) {
//		String queue_null_key = String.format(many_chat_queue_status_null_key, chatInfo.projectName,
//				table_name, status);
//		return cache.exists(queue_null_key);
//	}

//	class ManyChatQueueRowMapper implements RowMapper<ManyChatQueue> {
//		@Override
//		public ManyChatQueue mapRow(ResultSet rs, int rowNum)
//				throws SQLException {
//			ManyChatQueue res = new ManyChatQueue();
//			res.setCgid(rs.getLong("cgid"));
//			res.setMid(rs.getLong("mid"));
//			res.setStatus(rs.getInt("status"));
//			res.setTime(rs.getLong("time"));
//			res.setUid(rs.getLong("uid"));
//			return res;
//		}
//	}
	
//	class ManyChatQueueResultSetExtractor implements ResultSetExtractor<ManyChatQueue>{
//		@Override
//		public ManyChatQueue extractData(ResultSet rs)
//				throws SQLException, DataAccessException {
//			if(rs.next()){
//				ManyChatQueue res = new ManyChatQueue();
//				res.setCgid(rs.getLong("cgid"));
//				res.setMid(rs.getLong("mid"));
//				res.setStatus(rs.getInt("status"));
//				res.setTime(rs.getLong("time"));
//				res.setUid(rs.getLong("uid"));
//				return res;
//			} else
//				return null;
//		}
//	}
}
