package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.quakoo.framework.ext.chat.model.UserStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.quakoo.framework.ext.chat.dao.UserInfoDao;
import com.quakoo.framework.ext.chat.model.UserInfo;

import javax.jws.soap.SOAPBinding;

/**
 * 用户信息DAO
 * class_name: UserInfoDaoImpl
 * package: com.quakoo.framework.ext.chat.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:58
 **/
public class UserInfoDaoImpl extends BaseDaoHandle implements UserInfoDao {

	private final static String user_info_object_key = "%s_user_info_object_%d";
	private final static String user_info_queue_key = "%s_user_info_%s_queue";
	private final static String user_info_queue_null_key = "%s_user_info_%s_queue_null";
	private final static String user_info_incr_key = "%s_user_info_%s_incr";
	private final static long max_queue_num = 50000l;

    private Logger logger = LoggerFactory.getLogger(UserInfoDaoImpl.class);


    private String getTable(long uid){
		long index = uid % chatInfo.user_info_table_names.size();
		return chatInfo.user_info_table_names.get((int) index);
	}

	/**
     * 创建登陆时间
	 * method_name: create_login_time
	 * params: [uid]
	 * return: double
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:59
	 **/
	public double create_login_time(long uid) throws Exception {
		String tableName = getTable(uid);
	    String key = String.format(user_info_incr_key, chatInfo.projectName, tableName);
		Long num = cache.incr(key);
		if(null == num) throw new IllegalStateException("create login time error");
		cache.expire(key, AbstractChatInfo.redis_overtime_short);
		DecimalFormat decimalFormat = new DecimalFormat("000");
		long current_time = System.currentTimeMillis();
		if(num/999 > 0){
			num = num%999;
		}
		double loginTime = Double.parseDouble(current_time+"."+decimalFormat.format(num));
		return loginTime;
	}

    /**
     * 缓存用户信息
	 * method_name: cache_user_info
	 * params: [uid, lastIndex, loginTime, userInfo]
	 * return: com.quakoo.framework.ext.chat.model.UserInfo
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:59
	 **/
    @Override
    public void cache_user_info(UserInfo userInfo) throws Exception {
        String tableName = this.getTable(userInfo.getUid());
        String object_key = String.format(user_info_object_key, chatInfo.projectName, userInfo.getUid());
        String queue_null_key = String.format(user_info_queue_null_key, chatInfo.projectName, tableName);
        String queue_key = String.format(user_info_queue_key, chatInfo.projectName, tableName);
        cache.delete(queue_null_key);
        cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, userInfo);
        if(cache.exists(queue_key)){
            cache.zaddObject(queue_key, userInfo.getLoginTime(), userInfo.getUid());
            long length = cache.zcard(queue_key);
            if(length > max_queue_num){
                cache.zremrangeByRank(queue_key, 0, (int)(length - max_queue_num -1));
            }
        }
    }

//    @Override
//    public UserInfo cache_user_info(long uid, double lastIndex, double loginTime, UserInfo userInfo) throws Exception {
//        String tableName = this.getTable(uid);
//        String object_key = String.format(user_info_object_key, chatInfo.projectName, uid);
//        String queue_null_key = String.format(user_info_queue_null_key, chatInfo.projectName, tableName);
//        String queue_key = String.format(user_info_queue_key, chatInfo.projectName, tableName);
//        cache.delete(queue_null_key);
//        if(null == userInfo) {
//            userInfo = new UserInfo();
//            userInfo.setLastIndex(lastIndex);
//            userInfo.setLoginTime(loginTime);
//            userInfo.setPromptIndex(0);
//            userInfo.setUid(uid);
//        } else {
//            userInfo.setLastIndex(lastIndex);
//            userInfo.setLoginTime(loginTime);
//        }
//        cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, userInfo);
//        if(cache.exists(queue_key)){
//            cache.zaddObject(queue_key, loginTime, uid);
//            long length = cache.zcard(queue_key);
//            if(length > max_queue_num){
//                cache.zremrangeByRank(queue_key, 0, (int)(length - max_queue_num -1));
//            }
//        }
//        return userInfo;
//    }

//    @Override
//    public UserInfo sync(long uid, double lastIndex, double loginTime, UserInfo userInfo) throws Exception {
//        String tableName = this.getTable(uid);
//        String object_key = String.format(user_info_object_key, chatInfo.projectName, uid);
//        String queue_null_key = String.format(user_info_queue_null_key, chatInfo.projectName, tableName);
//        String queue_key = String.format(user_info_queue_key, chatInfo.projectName, tableName);
//        cache.delete(queue_null_key);
//        boolean mysqlSign = true;
//        Object exists = cache.getObject(object_key, null);
//        if(null != exists) {
//            UserInfo cacheUserInfo = (UserInfo) exists;
//            if(System.currentTimeMillis() - cacheUserInfo.getPersistentTime() < 1000 * 60 * 2) {
//                mysqlSign = false;
//            }
//        }
//
//        if (null == userInfo) {
//            userInfo = new UserInfo();
//            userInfo.setLastIndex(lastIndex);
//            userInfo.setLoginTime(loginTime);
//            userInfo.setPromptIndex(0);
//            userInfo.setUid(uid);
//            if(mysqlSign) {
//                userInfo.setPersistentTime(System.currentTimeMillis());
//                this.insert(userInfo);
//            }
//        } else {
//            userInfo.setLastIndex(lastIndex);
//            userInfo.setLoginTime(loginTime);
//            if(mysqlSign) {
//                userInfo.setPersistentTime(System.currentTimeMillis());
//                this.update(userInfo);
//            }
//        }
//        cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, userInfo);
//
//        if(cache.exists(queue_key)){
//            cache.zaddObject(queue_key, loginTime, uid);
//            long length = cache.zcard(queue_key);
//            if(length > max_queue_num){
//                cache.zremrangeByRank(queue_key, 0, (int)(length - max_queue_num -1));
//            }
//        }
//        return userInfo;
//    }

    /**
     * 替换用户信息
     * method_name: replace
     * params: [userInfos]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 16:59
     **/
    @Override
    public void replace(List<UserInfo> userInfos) throws DataAccessException {
        String sqlPrev = "insert into %s (uid, lastIndex, promptIndex, loginTime) values ";
        String sqlValueFormat = "(%d, %s, %s, %s)";
        String sqlSuffix = " on duplicate key update lastIndex=values(lastIndex), promptIndex=values(promptIndex), loginTime=values(loginTime)";
        Map<String, List<UserInfo>> maps = Maps.newHashMap();
        for(UserInfo userInfo : userInfos){
            String tableName = getTable(userInfo.getUid());
            List<UserInfo> list = maps.get(tableName);
            if (null == list) {
                list = Lists.newArrayList();
                maps.put(tableName, list);
            }
            list.add(userInfo);
        }
        List<String> sqls = Lists.newArrayList();
        for(Entry<String, List<UserInfo>> entry : maps.entrySet()){
            String tableName = entry.getKey();
            List<UserInfo> list = entry.getValue();
            List<String> sqlValueList = Lists.newArrayList();
            for (UserInfo userInfo : list) {
                String sqlValue = String.format(sqlValueFormat, userInfo.getUid(), userInfo.getLastIndex(),
                        userInfo.getPromptIndex(), userInfo.getLoginTime());
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
//    public void replace(List<UserInfo> userInfos) throws DataAccessException {
//        try {
//            String sqlPrev = "replace into %s (uid, lastIndex, promptIndex, loginTime) values ";
//            String sqlValueFormat = "(%d, %s, %s, %s)";
//            Map<String, List<UserInfo>> maps = Maps.newHashMap();
//            for(UserInfo userInfo : userInfos){
//                String tableName = getTable(userInfo.getUid());
//                List<UserInfo> list = maps.get(tableName);
//                if(null == list){
//                    list = Lists.newArrayList();
//                    maps.put(tableName, list);
//                }
//                list.add(userInfo);
//            }
//            List<String> sqls = Lists.newArrayList();
//            for(Entry<String, List<UserInfo>> entry : maps.entrySet()){
//                String tableName = entry.getKey();
//                List<UserInfo> list = entry.getValue();
//                List<String> sqlValueList = Lists.newArrayList();
//                for(UserInfo userInfo : list){
//                    String sqlValue = String.format(sqlValueFormat, userInfo.getUid(), userInfo.getLastIndex(),
//                            userInfo.getPromptIndex(), userInfo.getLoginTime());
//                    sqlValueList.add(sqlValue);
//                }
//                String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
//                String sql = sqlPrev + sqlValues;
//                sql = String.format(sql, tableName);
//                sqls.add(sql);
//            }
//            long startTime = System.currentTimeMillis();
//            int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString()
//                    + " res : " + ArrayUtils.toString(resList));
//        } finally {
//        }
//    }

    public static void main(String[] args) {
        int[] a  = new int[2];
        a[0] = 1;
        a[1] =2;
        System.out.println(ArrayUtils.toString(a));
    }

    private UserInfo insert(UserInfo userInfo) throws DataAccessException {
		long uid = userInfo.getUid();
		double lastIndex = userInfo.getLastIndex();
		double promptIndex = userInfo.getPromptIndex();
		double loginTime = userInfo.getLoginTime();
		String tableName = this.getTable(uid);
		boolean sign = false;
		String sql = "insert ignore into %s (uid, lastIndex, promptIndex, loginTime) " +
				"values (?, ?, ?, ?)";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, uid, lastIndex, promptIndex, loginTime);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
		if(ret > 0){
			return userInfo;
		} else {
			return null;
		}
	}

    private boolean update(UserInfo userInfo) throws DataAccessException {
        String tableName = this.getTable(userInfo.getUid());
        String sql = "update %s set loginTime = %s, lastIndex = %s where uid = %d";
        sql = String.format(sql, tableName, userInfo.getLoginTime(), userInfo.getLastIndex(), userInfo.getUid());
        long startTime = System.currentTimeMillis();
        int ret = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        boolean res = ret > 0 ? true : false;
        return res;
    }

	public UserInfo load(long uid) throws DataAccessException {
		String object_key = String.format(user_info_object_key, chatInfo.projectName, uid);
		Object obj = cache.getObject(object_key, null);
		if(null != obj){
			return (UserInfo) obj;
		}else{
			String tableName = this.getTable(uid);
			String sql = "select * from %s where uid = %d";
			sql = String.format(sql, tableName, uid);
			long startTime = System.currentTimeMillis();
			UserInfo userInfo = this.jdbcTemplate.query(sql, new UserInfoResultSetExtractor());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

            if(null != userInfo){
				cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, userInfo);
			}
			return userInfo;
		}
	}

    @Override
    public List<UserInfo> load_cache(List<Long> uids) throws DataAccessException {
        List<String> keys = Lists.newArrayList();
        for(long uid : uids) {
            String object_key = String.format(user_info_object_key, chatInfo.projectName, uid);
            keys.add(object_key);
        }
        Map<String, Object> redis_map = cache.multiGetObject(keys, null);
        List<UserInfo> res = Lists.newArrayList();
        for(Object obj : redis_map.values()) {
            if(obj != null) {
                res.add((UserInfo) obj);
            }
        }
        return res;
    }

    /**
     * 批量获取用户信息
	 * method_name: loads
	 * params: [uids]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.UserInfo>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:00
	 **/
	public List<UserInfo> loads(List<Long> uids) throws DataAccessException {
		List<String> object_key_list = Lists.newArrayList();
		Map<Long, String> id_key_map = Maps.newHashMap();
		for(long uid : uids){
			String object_key = String.format(user_info_object_key, chatInfo.projectName, uid);
			object_key_list.add(object_key);
			id_key_map.put(uid, object_key);
		}
		Map<Long, UserInfo> res_map = Maps.newHashMap();
		Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
		List<Long> non_uids = Lists.newArrayList();
		for(long uid : uids){
			Object obj = redis_map.get(id_key_map.get(uid));
			if(null == obj){
				non_uids.add(uid);
			} else {
				res_map.put(uid, (UserInfo) obj);
			}
		}
		
		if(non_uids.size() > 0){
			String sql = "select * from %s where uid in (%s)";
			Map<String, List<Long>> maps = Maps.newHashMap();
			for(long uid : non_uids){
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
			
			Map<String, Object> redis_insert_map = Maps.newHashMap();
			for(String one_sql : sqls){
			    long startTime = System.currentTimeMillis();
				List<UserInfo> list = this.jdbcTemplate.query(one_sql, new UserInfoRowMapper());
                logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + one_sql);

                for(UserInfo userInfo : list){
					long uid = userInfo.getUid();
					res_map.put(uid, userInfo);
					redis_insert_map.put(String.format(user_info_object_key, chatInfo.projectName, uid), userInfo);
				}
			}
			cache.multiSetObject(redis_insert_map, AbstractChatInfo.redis_overtime_long);
		}
		List<UserInfo> res = Lists.newArrayList();
		for(long uid : uids){
			UserInfo userInfo = res_map.get(uid);
			res.add(userInfo);
		}
		return res;
	}
	

	
	public boolean update_prompt_index(long uid, double promptIndex)
			throws DataAccessException {
		String tableName = this.getTable(uid);
		String sql = "update %s set promptIndex = ? where uid = ?";
	    sql = String.format(sql, tableName);
	    long startTime = System.currentTimeMillis();
	    int ret = this.jdbcTemplate.update(sql, promptIndex, uid);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        boolean res = ret > 0 ? true : false;
	    if(res){
	    	String object_key = String.format(user_info_object_key, chatInfo.projectName, uid);
	    	Object obj = this.cache.getObject(object_key, null);
	    	if(null != obj){
	    		UserInfo userInfo = (UserInfo) obj;
	    		userInfo.setPromptIndex(promptIndex);
	    		cache.setObject(object_key, AbstractChatInfo.redis_overtime_long, userInfo);
	    	}
	    }
		return res;
	}
	
	public List<UserInfo> list(String table_name, double loginTime, int size)
			throws DataAccessException {
		this.init(table_name);
		String queue_key = String.format(user_info_queue_key, chatInfo.projectName, table_name);
		String queue_null_key = String.format(user_info_queue_null_key, chatInfo.projectName, table_name);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			Set<Object> set = this.cache.zrevrangeByScoreObject(queue_key, loginTime, 0,  0, size, null);
			List<UserInfo> userInfos = Lists.newArrayList();
			if(set.size() > 0){
				List<Long> uids = Lists.newArrayList();
				for(Object one : set){
					uids.add((long) one);
				}
				userInfos = this.loads(uids);
			}
			if(userInfos.size() == size) return userInfos;
			long redis_queue_len = this.cache.zcard(queue_key);
			if(redis_queue_len < max_queue_num)	return userInfos;
		}
		String sql = "select * from %s where loginTime <= %s order by loginTime desc limit %d";
		sql = String.format(sql, table_name, loginTime, size);
		long startTime = System.currentTimeMillis();
		List<UserInfo> userInfos = this.jdbcTemplate.query(sql, new UserInfoRowMapper());
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        return userInfos;
	}
	
	private void init(String table_name){
		String queue_key = String.format(user_info_queue_key, chatInfo.projectName, table_name);
		String queue_null_key = String.format(user_info_queue_null_key, chatInfo.projectName, table_name);
		if(!cache.exists(queue_key) && !cache.exists(queue_null_key)){
			ZkLock lock = null;
			try {
				lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
						chatInfo.projectName, queue_key + AbstractChatInfo.lock_suffix, 
						true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
				if(!cache.exists(queue_key) && !cache.exists(queue_null_key)){
					String sql = "select * from %s order by loginTime desc limit %d";
					sql = String.format(sql, table_name, max_queue_num);
					long startTime = System.currentTimeMillis();
					List<UserInfo> list = this.jdbcTemplate.query(sql, new UserInfoRowMapper());
                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

                    if(null != list && list.size() > 0){
						Map<Object, Double> map = Maps.newHashMap();
				    	for(UserInfo one : list){
				    		Double score = new Double(one.getLoginTime());
				    		map.put(one.getUid(), score);
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
	
	class UserInfoResultSetExtractor implements ResultSetExtractor<UserInfo> {
		@Override
		public UserInfo extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if(rs.next()){
				UserInfo res = new UserInfo();
				res.setLastIndex(rs.getDouble("lastIndex"));
				res.setLoginTime(rs.getDouble("loginTime"));
				res.setPromptIndex(rs.getDouble("promptIndex"));
				res.setUid(rs.getLong("uid"));
				return res;
			} else  
				return null;
		}
	}

	class UserInfoRowMapper implements RowMapper<UserInfo> {
		@Override
		public UserInfo mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			UserInfo res = new UserInfo();
			res.setLastIndex(rs.getDouble("lastIndex"));
			res.setLoginTime(rs.getDouble("loginTime"));
			res.setPromptIndex(rs.getDouble("promptIndex"));
			res.setUid(rs.getLong("uid"));
			return res;
		}
	}

}
