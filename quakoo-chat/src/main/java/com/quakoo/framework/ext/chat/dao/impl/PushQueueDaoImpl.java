package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.PushQueueDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.framework.ext.chat.model.PushQueue;

public class PushQueueDaoImpl extends BaseDaoHandle implements PushQueueDao {
	
	private final static String push_queue_status_key = "%s_push_queue_status_%d";
	private final static String push_queue_status_null_key = "%s_push_queue_status_%d_null";

	Logger logger = LoggerFactory.getLogger(PushQueueDaoImpl.class);
	
    @Resource
    private DataFieldMaxValueIncrementer pushQueueMaxValueIncrementer;
    
	public boolean insert(PushQueue one) throws DataAccessException {
		boolean res = false;
		long id = pushQueueMaxValueIncrementer.nextLongValue();
		one.setId(id);
		long uid = one.getUid();
		long mid = one.getMid();
		long time = one.getTime();
		int status = one.getStatus();
		String sql = "insert ignore into push_queue (id, uid, mid, status, time) values (?, ?, ?, ?, ?)";
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, id, uid, mid, status, time);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        res = ret > 0 ? true : false;
		
		if(res){
			String queue_key = String.format(push_queue_status_key, chatInfo.projectName, status);
			String queue_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, status);
			cache.delete(queue_null_key);
			if(cache.exists(queue_key)){
				cache.zaddObject(queue_key, new Double(time), one);
			}
		}
		return res;
	}

    public int insert(List<PushQueue> list) throws DataAccessException {
		for(PushQueue one : list) {
			long id = pushQueueMaxValueIncrementer.nextLongValue();
			one.setId(id);
		}
		String sqlPrev = "insert ignore into push_queue (id, uid, mid, status, `time`) values ";
		String sqlValueFormat = "(%d, %d, %d, %d, %d)";
		List<String> sqlValueList = Lists.newArrayList();
		for(PushQueue one : list) {
			String sqlValue = String.format(sqlValueFormat, one.getId(), one.getUid(), one.getMid(),
					one.getStatus(), one.getTime());
			sqlValueList.add(sqlValue);
		}
		String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
		String sql = sqlPrev + sqlValues;
        long startTime = System.currentTimeMillis();
		int res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        Set<String> queue_key_set = Sets.newHashSet();
		Set<String> null_queue_key_set = Sets.newHashSet();
		for(PushQueue one : list) {
			String queue_key = String.format(push_queue_status_key, chatInfo.projectName, one.getStatus());
			queue_key_set.add(queue_key);
			String null_queue_key = String.format(push_queue_status_null_key, chatInfo.projectName, one.getStatus());
			null_queue_key_set.add(null_queue_key);
		}
		cache.multiDelete(Lists.newArrayList(null_queue_key_set));
		if(list.size() != res){
		     cache.multiDelete(Lists.newArrayList(queue_key_set));
		} else {
			Map<String, Boolean> exists_map = cache.pipExists(Lists.newArrayList(queue_key_set));
			Map<String, Map<Object, Double>> insert_map = Maps.newHashMap();
			for(PushQueue one : list) {
				String queue_key = String.format(push_queue_status_key, chatInfo.projectName, one.getStatus());
				if(exists_map.get(queue_key).booleanValue()) {
					Map<Object, Double> map = insert_map.get(queue_key);
					if(null == map) {
						map = Maps.newHashMap();
						insert_map.put(queue_key, map);
					}
					map.put(one, new Double(one.getTime()));
				}
			}
			if(insert_map.size() > 0) {
				for(Entry<String, Map<Object, Double>> entry : insert_map.entrySet()) {
					String key = entry.getKey();
					Map<Object, Double> map = entry.getValue();
					if(map.size() > 0) {
						cache.zaddMultiObject(key, map);
					}
				}
			}
		}
		return res;
	}

    @Override
    public boolean update(List<PushQueue> list, int oldStatus, int newStatus) throws DataAccessException {
	    List<String> sqls = Lists.newArrayList();
	    for(PushQueue one : list) {
            String sqlFormat = "update push_queue set status = %d where id = %d and status = %d ";
            sqls.add(String.format(sqlFormat, newStatus, one.getId(), oldStatus));
        }
        long startTime = System.currentTimeMillis();
        int[] res = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sqls.toString());
        int totalRes = 0;
	    for(int one : res) totalRes += one;
	    if(totalRes == list.size()) {
            String queue_key = String.format(push_queue_status_key, chatInfo.projectName, oldStatus);
            if(cache.exists(queue_key)) {
                List<Object> rems = Lists.newArrayList();
                for(PushQueue one : list) {
                    rems.add(one);
                }
                if(rems.size() > 0) {
                    cache.zremMultiObject(queue_key, rems);
                    String queue_new_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, newStatus);
                    cache.delete(queue_new_null_key);
                }
            }
            String queue_new_key = String.format(push_queue_status_key, chatInfo.projectName, newStatus);
            if(cache.exists(queue_new_key)){
                Map<Object, Double> adds = Maps.newHashMap();
                for(PushQueue one : list) {
                    one.setStatus(newStatus);
                    adds.put(one, new Double(one.getTime()));
                }
                if(adds.size() > 0) cache.zaddMultiObject(queue_new_key, adds);
            }
        } else {
            String queue_key = String.format(push_queue_status_key, chatInfo.projectName, oldStatus);
            cache.delete(queue_key);
            String queue_new_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, newStatus);
            cache.delete(queue_new_null_key);
            String queue_new_key = String.format(push_queue_status_key, chatInfo.projectName, newStatus);
            cache.delete(queue_new_key);
            String queue_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, oldStatus);
            cache.delete(queue_null_key);
        }
        return false;
    }

    public boolean update(PushQueue one, int newStatus)
			throws DataAccessException {
		boolean res = false;
		long id = one.getId();
		long time = one.getTime();
		int status = one.getStatus();
		String sql = "update push_queue set status = ? where id = ?";
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, newStatus, id);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
		res = ret > 0 ? true : false;
		
		if(res){
			String queue_key = String.format(push_queue_status_key, chatInfo.projectName, status);
			if(cache.exists(queue_key))
				cache.zremObject(queue_key, one);
			String queue_new_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, newStatus);
			cache.delete(queue_new_null_key);
			String queue_new_key = String.format(push_queue_status_key, chatInfo.projectName, newStatus);
			if(cache.exists(queue_new_key)){
				one.setStatus(newStatus);
				cache.zaddObject(queue_new_key, new Double(time), one);
			}
		}
		return res;
	}

//    @Override
//    public void clear(long uid) throws DataAccessException {
//        String queue_key = String.format(push_queue_status_key, chatInfo.projectName, Status.unfinished);
//        String sql = "select * from push_queue where uid = %d";
//        sql = String.format(sql, uid);
//        List<PushQueue> all_list = this.jdbcTemplate.query(sql, new PushQueueRowMapper());
//        if(!cache.exists(queue_key)) {
//            List<Object> rems = Lists.newArrayList();
//            for(PushQueue pushQueue : all_list) {
//                rems.add(pushQueue);
//            }
//            if(rems.size() > 0) cache.zremMultiObject(queue_key, rems);
//        }
//        sql = "delete from push_queue where uid = %d";
//        sql = String.format(sql, uid);
//        long startTime = System.currentTimeMillis();
//        this.jdbcTemplate.update(sql);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//    }
	
	private void init(int status) {
		String queue_key = String.format(push_queue_status_key, chatInfo.projectName, status);
		String queue_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, status);
		if(!cache.exists(queue_null_key) && !cache.exists(queue_key)) {
			String sql = "select * from push_queue where status = %d order by time asc";
			sql = String.format(sql, status);
			long startTime = System.currentTimeMillis();
			List<PushQueue> all_list = this.jdbcTemplate.query(sql, new PushQueueRowMapper());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
			if(null != all_list && all_list.size() > 0) {
				Map<Object, Double> map = Maps.newHashMap();
		    	for(PushQueue one : all_list){
		    		Double score = new Double(one.getTime());
		    		map.put(one, score);
		    	}
		    	if(map.size() > 0){
		    		cache.zaddMultiObject(queue_key, map);
		    		cache.expire(queue_key, AbstractChatInfo.redis_overtime_long);
		    	}

			} else {
				cache.setString(queue_null_key, AbstractChatInfo.redis_overtime_long, "true");
			}
		}
	}

	public List<PushQueue> all_list(int status, int size)
			throws DataAccessException {
		this.init(status);
		String queue_key = String.format(push_queue_status_key, chatInfo.projectName, status);
		String queue_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, status);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)) {
			List<PushQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, Double.MAX_VALUE, 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((PushQueue) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}

	public List<PushQueue> list_time(int status, long maxTime, int size)
			throws DataAccessException {
		this.init(status);
		String queue_key = String.format(push_queue_status_key, chatInfo.projectName, status);
		String queue_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, status);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			List<PushQueue> res = Lists.newArrayList();
			Set<Object> set = cache.zrangeByScoreObject(queue_key, 0, new Double(maxTime), 0, size, null);
			if(null != set && set.size() > 0){
			    for(Object obj : set){
			   		res.add((PushQueue) obj);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}

	public boolean list_null(int status) {
		String queue_null_key = String.format(push_queue_status_null_key, chatInfo.projectName, status);
		return cache.exists(queue_null_key);
	}
	
	class PushQueueRowMapper implements RowMapper<PushQueue> {
		@Override
		public PushQueue mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			PushQueue res = new PushQueue();
			res.setMid(rs.getLong("mid"));
			res.setId(rs.getLong("id"));
			res.setStatus(rs.getInt("status"));
			res.setTime(rs.getLong("time"));
			res.setUid(rs.getLong("uid"));
			return res;
		}
	}
}
