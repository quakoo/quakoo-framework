package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.baseFramework.redis.util.HessianSerializeUtil;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.model.UserStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.baseFramework.redis.RedisSortData.RedisKeySortMemObj;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.UserDirectoryDao;
import com.quakoo.framework.ext.chat.model.UserDirectory;

/**
 * 用户目录DAO
 * class_name: UserDirectoryDaoImpl
 * package: com.quakoo.framework.ext.chat.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:57
 **/
public class UserDirectoryDaoImpl extends BaseDaoHandle implements UserDirectoryDao {

    private final static String user_directory_object_key = "%s_user_directory_object_uid_%d_type_%d_thirdId_%d";
	private static final String user_directory_list_key = "%s_user_directory_list_uid_%d";
	private static final String user_directory_list_null_key = "%s_user_directory_list_uid_%d_null";

    private Logger logger = LoggerFactory.getLogger(UserDirectoryDaoImpl.class);

    /**
     * 获取表名(根据UID获取表名)
     * method_name: getTable
     * params: [uid]
     * return: java.lang.String
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 16:57
     **/
	private String getTable(long uid){
		int index = (int) uid % chatInfo.user_directory_table_names.size();
		return chatInfo.user_directory_table_names.get(index);
	}

//	@Override
//	public void insert(UserDirectory messageDirectory)
//			throws DataAccessException {
//		boolean sign = false;
//		long uid = messageDirectory.getUid();
//		long type = messageDirectory.getType();
//		long thirdId = messageDirectory.getThirdId();
//		long ctime = messageDirectory.getCtime();
//		String tableName = getTable(uid);
//		String sql = "insert ignore into %s (uid, `type`, thirdId, ctime) values (?, ?, ?, ?)";
//		sql = String.format(sql, tableName);
//		long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, type, thirdId, ctime);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//        sign = ret > 0 ? true : false;
//		if(sign){
//			String list_key = String.format(user_directory_list_key, chatInfo.projectName, uid);
//			String list_null_key = String.format(user_directory_list_null_key, chatInfo.projectName, uid);
//			cache.delete(list_null_key);
//			if(cache.exists(list_key)){
//				cache.zaddObject(list_key, new Double(ctime), messageDirectory);
//			}
//		}
//	}

    /**
     * 批量添加
     * method_name: insert
     * params: [messageDirectories]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 16:57
     **/
    @Override
    public void insert(List<UserDirectory> messageDirectories) throws DataAccessException {
        chatInfo.segmentLock.lock(messageDirectories);
        try {
            List<String> sqls = Lists.newArrayList();
            String sql = "insert ignore into %s (uid, `type`, thirdId, ctime) values (%d, %d, %d, %d)";
            for (UserDirectory directory : messageDirectories) {
                long uid = directory.getUid();
                long thirdId = directory.getThirdId();
                int type = directory.getType();
                long ctime = directory.getCtime();
                String tableName = getTable(uid);
                String sqlOne = String.format(sql, tableName, uid, type, thirdId, ctime);
                sqls.add(sqlOne);
            }
            long startTime = System.currentTimeMillis();
            int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
            Set<String> list_key_set = Sets.newHashSet();
            Set<String> list_null_set = Sets.newHashSet();
            for (UserDirectory one : messageDirectories) {
                String key = String.format(user_directory_list_key, chatInfo.projectName, one.getUid());
                String null_key = String.format(user_directory_list_null_key, chatInfo.projectName, one.getUid());
                list_key_set.add(key);
                list_null_set.add(null_key);
            }
            if (list_null_set.size() > 0) {
                cache.multiDelete(Lists.newArrayList(list_null_set));
            }
            if (messageDirectories.size() != resList.length) {
                cache.multiDelete(Lists.newArrayList(list_key_set));
            } else {
                list_key_set.clear();
                for (int i = 0; i < resList.length; i++) {
                    UserDirectory one = messageDirectories.get(i);
                    int num = resList[i];
                    if (num > 0) {
                        String key = String.format(user_directory_list_key, chatInfo.projectName, one.getUid());
                        list_key_set.add(key);
                    }
                }
                Map<String, Boolean> exists_map = cache.pipExists(Lists.newArrayList(list_key_set));
                List<RedisKeySortMemObj> redisParams = Lists.newArrayList();
                Map<String, Object> objectRedisMap = Maps.newHashMap();
                for (int i = 0; i < resList.length; i++) {
                    UserDirectory one = messageDirectories.get(i);
                    int num = resList[i];
                    if (num > 0) {
                        String key = String.format(user_directory_list_key, chatInfo.projectName, one.getUid());
                        if (exists_map.get(key)) {
                            RedisKeySortMemObj redisParam = new RedisKeySortMemObj(key, one, new Double(one.getCtime()));
                            redisParams.add(redisParam);
                        }
                        String objecKey = String.format(user_directory_object_key, chatInfo.projectName, one.getUid(), one.getType(), one.getThirdId());
                        objectRedisMap.put(objecKey, one);
                    }
                }
                if (redisParams.size() > 0) cache.pipZaddObject(redisParams);
                if (objectRedisMap.size() > 0) cache.multiSetObject(objectRedisMap, AbstractChatInfo.redis_overtime_long);
            }
        } finally {
            chatInfo.segmentLock.unlock(messageDirectories);
        }
    }

	private void init(long uid) {
		String list_key = String.format(user_directory_list_key, chatInfo.projectName, uid);
		String list_null_key = String.format(user_directory_list_null_key, chatInfo.projectName, uid);
		boolean list_key_sign = cache.exists(list_key);
        boolean list_null_key_sign = cache.exists(list_null_key);
		if(!list_key_sign && !list_null_key_sign){
			ZkLock lock = null;
			try {
				lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
						chatInfo.projectName, list_key + AbstractChatInfo.lock_suffix,
						true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
				if(!cache.exists(list_null_key) && !cache.exists(list_key)){
					String tableName = getTable(uid);
					String sql = "select * from %s where uid = %d order by ctime desc";
					sql = String.format(sql, tableName, uid);
					long startTime = System.currentTimeMillis();
					List<UserDirectory> all_list = this.jdbcTemplate.query(sql, new UserDirectoryRowMapper());
                    logger.info(" ===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
                    if(null != all_list && all_list.size() > 0){
						Map<Object, Double> map = Maps.newHashMap();
				    	for(UserDirectory one : all_list){
				    		Double score = new Double(one.getCtime());
				    		map.put(one, score);
				    	}
				    	if(map.size() > 0){
				    		cache.zaddMultiObject(list_key, map);
				    		cache.expire(list_key, AbstractChatInfo.redis_overtime_long);
				    	}
					} else {
						cache.setString(list_null_key, AbstractChatInfo.redis_overtime_long, "true");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (lock != null) lock.release();
			}
		}
	}

	/**
     * 批量读取
	 * method_name: load
	 * params: [ids]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.UserDirectory>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 16:58
	 **/
    @Override
    public List<UserDirectory> load(List<UserDirectory> ids) throws DataAccessException {
        List<String> object_key_list = Lists.newArrayList();
        for(UserDirectory id : ids){
            String object_key = String.format(user_directory_object_key, chatInfo.projectName, id.getUid(), id.getType(), id.getThirdId());
            object_key_list.add(object_key);
        }
        Map<String, UserDirectory> res_map = Maps.newHashMap();
        Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
        List<UserDirectory> non_ids = Lists.newArrayList();
        for(UserDirectory id : ids){
            String object_key = String.format(user_directory_object_key, chatInfo.projectName, id.getUid(), id.getType(), id.getThirdId());
            Object obj = redis_map.get(object_key);
            if(null == obj){
                non_ids.add(id);
            } else {
                res_map.put(String.format("%d_%d_%d", id.getUid(), id.getType(), id.getThirdId()), (UserDirectory) obj);
            }
        }
        if(non_ids.size() > 0){
            String sqlPrev = "select * from %s where (uid, type, thirdId) in ";
            String sqlFormat = "(%d, %d, %d)";
            Map<String, List<UserDirectory>> maps = Maps.newHashMap();
            for(UserDirectory no_id : non_ids){
                String tableName = getTable(no_id.getUid());
                List<UserDirectory> list = maps.get(tableName);
                if(null == list){
                    list = Lists.newArrayList();
                    maps.put(tableName, list);
                }
                list.add(no_id);
            }
            List<String> sqls = Lists.newArrayList();
            for(Map.Entry<String, List<UserDirectory>> entry : maps.entrySet()){
                String tableName = entry.getKey();
                List<UserDirectory> list = entry.getValue();
                List<String> sqlValues = Lists.newArrayList();
                for(UserDirectory one : list) {
                    String sqlValue = String.format(sqlFormat, one.getUid(), one.getType(), one.getThirdId());
                    sqlValues.add(sqlValue);
                }
                String sqlParam = "(" + StringUtils.join(sqlValues, ",") + ")";
                String sql = String.format(sqlPrev, tableName) + sqlParam;
                sqls.add(sql);
            }

            Map<String, Object> redis_insert_map = Maps.newHashMap();
            for(String one_sql : sqls){
                long startTime = System.currentTimeMillis();
                List<UserDirectory> list = this.jdbcTemplate.query(one_sql, new UserDirectoryRowMapper());
                logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + one_sql);
                for(UserDirectory one : list){
                    res_map.put(String.format("%d_%d_%d", one.getUid(), one.getType(), one.getThirdId()), one);
                    redis_insert_map.put(String.format(user_directory_object_key, chatInfo.projectName,
                            one.getUid(), one.getType(), one.getThirdId()), one);
                }
            }
            cache.multiSetObject(redis_insert_map, AbstractChatInfo.redis_overtime_long);
        }
        List<UserDirectory> res = Lists.newArrayList();
        for(UserDirectory id : ids){
            UserDirectory userDirectory = res_map.get(String.format("%d_%d_%d", id.getUid(), id.getType(), id.getThirdId()));
            res.add(userDirectory);
        }
        return res;
    }

    @Override
	public List<UserDirectory> list_all(long uid) throws DataAccessException {
		this.init(uid);
		String list_key = String.format(user_directory_list_key, chatInfo.projectName, uid);
		String list_null_key = String.format(user_directory_list_null_key, chatInfo.projectName, uid);
		if(cache.exists(list_null_key))
			return Lists.newArrayList();
		if(cache.exists(list_key)){
			List<UserDirectory> res = Lists.newArrayList();
			Set<Object> set = cache.zrevrangeByScoreObject(list_key, Double.MAX_VALUE, 0, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((UserDirectory) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}

    class UserDirectoryRowMapper implements RowMapper<UserDirectory> {
		@Override
		public UserDirectory mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			UserDirectory res = new UserDirectory();
			res.setCtime(rs.getLong("ctime"));
			res.setThirdId(rs.getLong("thirdId"));
			res.setType(rs.getInt("type"));
			res.setUid(rs.getLong("uid"));
			return res;
		}
	}
}
