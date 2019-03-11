package com.quakoo.framework.ext.push.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.collect.Sets;
//import com.quakoo.framework.ext.push.model.PushHandleQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.redis.RedisAddSetParam;
import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushUserInfoPoolDao;
import com.quakoo.framework.ext.push.model.PushUserInfoPool;

/**
 * 推送用户信息DAO
 * class_name: PushUserInfoPoolDaoImpl
 * package: com.quakoo.framework.ext.push.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:37
 **/
public class PushUserInfoPoolDaoImpl extends BaseDao implements PushUserInfoPoolDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(PushUserInfoPoolDaoImpl.class);

	private String push_user_info_pool_key;
	private String push_user_info_pool_null_key;

    @Override
    public void afterPropertiesSet() throws Exception {
        push_user_info_pool_key = pushInfo.projectName + "_push_user_info_pool_%d";
        push_user_info_pool_null_key = pushInfo.projectName + "_push_user_info_pool_%d_null";
    }

    /**
     * 获取表名(多张表根据UID获取表名)
     * method_name: getTable
     * params: [uid]
     * return: java.lang.String
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:37
     **/
    private String getTable(long uid){
		int index = (int) uid % pushInfo.push_user_info_pool_table_names.size();
		return pushInfo.push_user_info_pool_table_names.get(index);
	}

    /**
     * 缓存插入
	 * method_name: cache_insert
	 * params: [one]
	 * return: boolean
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:42
	 **/
    @Override
    public boolean cache_insert(PushUserInfoPool one) throws DataAccessException {
        long uid = one.getUid();
        String pool_key = String.format(push_user_info_pool_key, uid);
        String pool_null_key = String.format(push_user_info_pool_null_key, uid);
        cache.multiDelete(Lists.newArrayList(pool_key, pool_null_key));
        one.setActiveTime(0);
        long res = cache.saddObject(pool_key, one);
        return res > 0 ? true : false;
    }

    /**
     * 插入数据库
     * method_name: insert
     * params: [pools]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:42
     **/
    @Override
    public void insert(List<PushUserInfoPool> pools) throws DataAccessException {
        String sqlPrev = "insert into %s (uid, platform, brand, sessionId, iosToken, huaWeiToken,  meiZuPushId, activeTime) values ";
        String sqlValueFormat = "(%d, %d, %d, '%s', '%s', '%s', '%s', %d)";
        String sqlSuffix = " on duplicate key update platform=values(platform), brand=values(brand), sessionId=values(sessionId), iosToken=values(iosToken), huaWeiToken=values(huaWeiToken), meiZuPushId=values(meiZuPushId), activeTime=values(activeTime)";
        Map<String, List<PushUserInfoPool>> maps = Maps.newHashMap();
        for (PushUserInfoPool pool : pools) {
            String tableName = getTable(pool.getUid());
            List<PushUserInfoPool> list = maps.get(tableName);
            if (null == list) {
                list = Lists.newArrayList();
                maps.put(tableName, list);
            }
            list.add(pool);
        }
        List<String> sqls = Lists.newArrayList();
        for (Entry<String, List<PushUserInfoPool>> entry : maps.entrySet()) {
            String tableName = entry.getKey();
            List<PushUserInfoPool> list = entry.getValue();
            List<String> sqlValueList = Lists.newArrayList();
            for (PushUserInfoPool one : list) {
                String sessionId = one.getSessionId();
                if(StringUtils.isBlank(sessionId)) sessionId = "";
                String iosToken = one.getIosToken();
                if(StringUtils.isBlank(iosToken)) iosToken = "";
                String huaWeiToken = one.getHuaWeiToken();
                if(StringUtils.isBlank(huaWeiToken)) huaWeiToken = "";
                String meiZuPushId = one.getMeiZuPushId();
                if(StringUtils.isBlank(meiZuPushId)) meiZuPushId = "";
                String sqlValue = String.format(sqlValueFormat, one.getUid(), one.getPlatform(), one.getBrand(), sessionId,
                        iosToken, huaWeiToken, meiZuPushId, one.getActiveTime());
                sqlValueList.add(sqlValue);
            }
            String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
            String sql = sqlPrev + sqlValues + sqlSuffix;
            sql = String.format(sql, tableName);
            sqls.add(sql);
        }
        long startTime = System.currentTimeMillis();
        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString()
                + " res : " + ArrayUtils.toString(resList));
    }
    //    @Override
//    public void insert(List<PushUserInfoPool> pools) throws DataAccessException {
//        String sql = "replace into %s (uid, platform, brand, sessionId, iosToken, huaWeiToken,  meiZuPushId, activeTime) values (?, ?, ?, ?, ?, ?, ?, ?)";
//        Map<String, List<PushUserInfoPool>> maps = Maps.newHashMap();
//        for (PushUserInfoPool pool : pools) {
//            String tableName = getTable(pool.getUid());
//            List<PushUserInfoPool> list = maps.get(tableName);
//            if (null == list) {
//                list = Lists.newArrayList();
//                maps.put(tableName, list);
//            }
//            list.add(pool);
//        }
//        for (Entry<String, List<PushUserInfoPool>> entry : maps.entrySet()) {
//            String tableName = entry.getKey();
//            String subSql = String.format(sql, tableName);
//            final List<PushUserInfoPool> subList = entry.getValue();
//            long startTime = System.currentTimeMillis();
//            int[] resList = this.jdbcTemplate.batchUpdate(subSql, new BatchPreparedStatementSetter() {
//                @Override
//                public void setValues(PreparedStatement ps, int i) throws SQLException {
//                    PushUserInfoPool one =  subList.get(i);
//                    ps.setLong(1, one.getUid());
//                    ps.setInt(2, one.getPlatform());
//                    ps.setInt(3, one.getBrand());
//                    ps.setString(4, one.getSessionId());
//                    ps.setString(5, one.getIosToken());
//                    ps.setString(6, one.getHuaWeiToken());
//                    ps.setString(7, one.getMeiZuPushId());
//                    ps.setLong(8, System.currentTimeMillis());
//                }
//                @Override
//                public int getBatchSize() {
//                    return subList.size();
//                }
//            });
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + subSql.toString()
//                    + "PushUserInfoPool : " + subList.toString());
//        }
//    }

//    @Override
//	public boolean insert(PushUserInfoPool one) throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		String tableName = this.getTable(uid);
//		long activeTime = System.currentTimeMillis();
//		int brand = one.getBrand();
//		int platform = one.getPlatform();
//		String sessionId = one.getSessionId();
//		String iosToken = one.getIosToken();
//		String huaWeiToken = one.getHuaWeiToken();
//		String meiZuPushId = one.getMeiZuPushId();
//		String sqlFormat = "replace into %s (uid, platform, brand, sessionId, iosToken, huaWeiToken,  meiZuPushId, activeTime) " +
//				"values (?, ?, ?, ?, ?, ?, ?, ?)";
//		String sql = String.format(sqlFormat, tableName);
//        long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, platform, brand, sessionId, iosToken, huaWeiToken, meiZuPushId, activeTime);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//		res = ret > 0 ? true : false;
//		if(res) {
//			String pool_null_key = String.format(push_user_info_pool_null_key, uid);
//			cache.delete(pool_null_key);
//			String pool_key = String.format(push_user_info_pool_key, uid);
//			if(cache.exists(pool_key)) {
//				one.setActiveTime(0);
//				cache.saddObject(pool_key, one);
//			}
//		}
//		return res;
//	}

//    @Override
//    public boolean clear(long uid) throws DataAccessException {
//        boolean res = false;
//        String tableName = this.getTable(uid);
//        String sqlFormat = "delete from %s where uid = ?";
//        String sql = String.format(sqlFormat, tableName);
//        long startTime = System.currentTimeMillis();
//        int ret = this.jdbcTemplate.update(sql, uid);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//        res = ret > 0 ? true : false;
//        if(res) {
//            String pool_key = String.format(push_user_info_pool_key, uid);
//            cache.delete(pool_key);
//        }
//        return res;
//    }


    /**
     * 清理缓存
     * method_name: cache_clear
     * params: [uid]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:42
     **/
    @Override
    public boolean cache_clear(long uid) throws DataAccessException {
        String pool_key = String.format(push_user_info_pool_key, uid);
        String pool_null_key = String.format(push_user_info_pool_null_key, uid);
        long res = cache.delete(pool_key);
        if(res > 0) {
            cache.setString(pool_null_key, pushInfo.redis_overtime_long, "true");
        }
        return res > 0 ? true : false;
    }


    /**
     * 数据库清理
     * method_name: clear
     * params: [uids]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:43
     **/
    @Override
    public void clear(List<Long> uids) throws DataAccessException {
        Set<String> pool_key_set = Sets.newHashSet();
        String sqlFormat = "delete from %s where uid in (%s)";
        Map<String, List<Long>> maps = Maps.newHashMap();
        for (long uid : uids) {
            String pool_key = String.format(push_user_info_pool_key, uid);
            pool_key_set.add(pool_key);
            String tableName = getTable(uid);
            List<Long> list = maps.get(tableName);
            if (null == list) {
                list = Lists.newArrayList();
                maps.put(tableName, list);
            }
            list.add(uid);
        }
        cache.multiDelete(Lists.newArrayList(pool_key_set));
        List<String> sqls = Lists.newArrayList();
        for (Entry<String, List<Long>> entry : maps.entrySet()) {
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
//	public boolean delete(PushUserInfoPool one) throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		String tableName = this.getTable(uid);
//		int brand = one.getBrand();
//		int platform = one.getPlatform();
//		String sessionId = one.getSessionId();
//		String sqlFormat = "delete from %s where uid = ? and platform = ? " +
//				"and brand = ? and sessionId = ?";
//		String sql = String.format(sqlFormat, tableName);
//        long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, platform, brand, sessionId);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//		res = ret > 0 ? true : false;
//		if(res) {
//			one.setActiveTime(0);
//			String pool_key = String.format(push_user_info_pool_key, uid);
//			if(cache.exists(pool_key)) {
//				cache.sremObject(pool_key, one);
//			}
//		}
//		return res;
//	}

    /**
     * 初始化多个用户
     * method_name: init
     * params: [uids]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:44
     **/
	private void init(List<Long> uids) {
		List<String> keys = Lists.newArrayList();
		for(long uid : uids) {
			String pool_null_key = String.format(push_user_info_pool_null_key, uid);
			String pool_key = String.format(push_user_info_pool_key, uid);
			keys.add(pool_key);
			keys.add(pool_null_key);
		}
		Map<String, Boolean> keyExistsMap = cache.pipExists(keys);
		List<Long> queryUids = Lists.newArrayList();
		for(long uid : uids) {
			String pool_null_key = String.format(push_user_info_pool_null_key, uid);
			String pool_key = String.format(push_user_info_pool_key, uid);
			if(!keyExistsMap.get(pool_null_key) && !keyExistsMap.get(pool_key)){
				queryUids.add(uid);
			}
		}
		if(queryUids.size() > 0) {
			String sql = "select * from %s where uid in (%s)";
			Map<String, List<Long>> maps = Maps.newHashMap();
			for(long uid : queryUids) {
				String tableName = getTable(uid);
				List<Long> list = maps.get(tableName);
				if(null == list){
					list = Lists.newArrayList();
					maps.put(tableName, list);
				}
				list.add(uid);
			}
			List<String> sqls = Lists.newArrayList();
			for(Entry<String, List<Long>> entry : maps.entrySet()){
				String tableName = entry.getKey();
				List<Long> list = entry.getValue();
				String one_sql = String.format(sql, tableName, StringUtils.join(list, ","));
				sqls.add(one_sql);
			}
			Map<Long, List<PushUserInfoPool>> user_info_pool_map = Maps.newHashMap();
			for(String one_sql : sqls){
                long startTime = System.currentTimeMillis();
				List<PushUserInfoPool> list = this.jdbcTemplate.query(one_sql,
						new PushUserInfoPoolRowMapper());
                logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
				for(PushUserInfoPool one : list) {
					long uid = one.getUid();
					List<PushUserInfoPool> pools = user_info_pool_map.get(uid);
					if(null == pools) {
						pools = Lists.newArrayList();
						user_info_pool_map.put(uid, pools);
					}
					pools.add(one);
				}
			}
			List<RedisAddSetParam> redisParams = Lists.newArrayList();
			Map<String, String> nullKeys = Maps.newHashMap();
			for(long uid : queryUids) {
				List<PushUserInfoPool> pools = user_info_pool_map.get(uid);
				if(null != pools) {
					RedisAddSetParam redisParam = new RedisAddSetParam();
					redisParam.setKey(String.format(push_user_info_pool_key, uid));
					List<Object> members = Lists.newArrayList();
					for(PushUserInfoPool pool : pools) {
						pool.setActiveTime(0);
						members.add(pool);
					}
					redisParam.setMembers(members);
					redisParams.add(redisParam);
				} else {
					nullKeys.put(String.format(push_user_info_pool_null_key, uid), "true");
				}
			}
			if(nullKeys.size() > 0) {
				cache.multiSetString(nullKeys, pushInfo.redis_overtime_long);
			}
			if(redisParams.size() > 0) {
				cache.pipSaddObject(redisParams);
				List<String> expireKeys = Lists.newArrayList();
				for(RedisAddSetParam param : redisParams) {
					expireKeys.add(param.getKey());
				}
				cache.pipExpire(expireKeys, pushInfo.redis_overtime_long);
			}
		}
	}
	
	
	/**
     * 初始化单个用户
	 * method_name: init
	 * params: [uid]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:44
	 **/
	private void init(long uid) {
		String pool_null_key = String.format(push_user_info_pool_null_key, uid);
		String pool_key = String.format(push_user_info_pool_key, uid);
		if(!cache.exists(pool_null_key) && !cache.exists(pool_key)){
			String tableName = this.getTable(uid);
			String sql = "select * from %s where uid = %d";
			sql = String.format(sql, tableName, uid);
            long startTime = System.currentTimeMillis();
			List<PushUserInfoPool> list = this.jdbcTemplate.query(sql,
					new PushUserInfoPoolRowMapper());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
			if(null != list && list.size() > 0){
				List<Object> members = Lists.newArrayList();
				for(PushUserInfoPool one : list) {
					one.setActiveTime(0);
					members.add(one);
				}
				cache.saddMultiObject(pool_key, members);
				cache.expire(pool_key, pushInfo.redis_overtime_long);
			} else {
				cache.setString(pool_null_key, pushInfo.redis_overtime_long, "true");
			}
		}
	}

	/**
     * 获取多个用户的推送信息
	 * method_name: getPushUserInfos
	 * params: [uids]
	 * return: java.util.Map<java.lang.Long,java.util.List<com.quakoo.framework.ext.push.model.PushUserInfoPool>>
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:45
	 **/
	@Override
	public Map<Long, List<PushUserInfoPool>> getPushUserInfos(List<Long> uids)
			throws DataAccessException {
		this.init(uids);
		List<String> keys = Lists.newArrayList();
		for(long uid : uids) {
			String pool_null_key = String.format(push_user_info_pool_null_key, uid);
			String pool_key = String.format(push_user_info_pool_key, uid);
			keys.add(pool_key);
			keys.add(pool_null_key);
		}
		Map<String, Boolean> keyExistsMap = cache.pipExists(keys);
		Map<Long, List<PushUserInfoPool>> res = Maps.newHashMap();
		List<String> setKeys = Lists.newArrayList();
		for(long uid : uids) {
			String pool_key = String.format(push_user_info_pool_key, uid);
			if(keyExistsMap.get(pool_key))
				setKeys.add(pool_key);
		}
		Map<String, Set<Object>> map = cache.pipSmembersObject(setKeys);
		for(long uid : uids) {
			String pool_key = String.format(push_user_info_pool_key, uid);
			if(keyExistsMap.get(pool_key)) {
				Set<Object> members = map.get(pool_key);
				List<PushUserInfoPool> list = Lists.newArrayList();
				for(Object member : members) {
					list.add((PushUserInfoPool) member);
				}
				res.put(uid, list);
			} else {
				res.put(uid, new ArrayList<PushUserInfoPool>());
			}
		}
		return res;
	}

	/**
     * 获取单个用户的推送信息
	 * method_name: getPushUserInfos
	 * params: [uid]
	 * return: java.util.List<com.quakoo.framework.ext.push.model.PushUserInfoPool>
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:46
	 **/
	@Override
	public List<PushUserInfoPool> getPushUserInfos(long uid)
			throws DataAccessException {
		this.init(uid);
		String pool_null_key = String.format(push_user_info_pool_null_key, uid);
		String pool_key = String.format(push_user_info_pool_key, uid);
		if(cache.exists(pool_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(pool_key)){
			List<PushUserInfoPool> res = Lists.newArrayList();
			Set<Object> set = cache.smemberObject(pool_key, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((PushUserInfoPool) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}
	
	class PushUserInfoPoolRowMapper implements RowMapper<PushUserInfoPool> {
		@Override
		public PushUserInfoPool mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			PushUserInfoPool res = new PushUserInfoPool();
			res.setActiveTime(rs.getLong("activeTime"));
			res.setBrand(rs.getInt("brand"));
			res.setPlatform(rs.getInt("platform"));
			res.setSessionId(rs.getString("sessionId"));
			res.setUid(rs.getLong("uid"));
			res.setIosToken(rs.getString("iosToken"));
			res.setHuaWeiToken(rs.getString("huaWeiToken"));
			res.setMeiZuPushId(rs.getString("meiZuPushId"));
			return res;

		}
	}

}
