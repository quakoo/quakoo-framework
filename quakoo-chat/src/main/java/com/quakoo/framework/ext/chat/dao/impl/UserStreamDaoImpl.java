package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.baseFramework.redis.RedisIncrParam;
import com.quakoo.baseFramework.redis.RedisSortedSetParam;
import com.quakoo.baseFramework.redis.RedisSortedSetZremrangeParam;
import com.quakoo.baseFramework.redis.RedisSortData.RedisKeySortMemObj;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.UserStreamDao;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.param.UserOneStreamParam;
import com.quakoo.framework.ext.chat.model.param.UserStreamParam;
import com.quakoo.framework.ext.chat.model.param.WillPushItem;

/**
 * 用户信息流DAO
 * class_name: UserStreamDaoImpl
 * package: com.quakoo.framework.ext.chat.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:00
 **/
public class UserStreamDaoImpl extends BaseDaoHandle implements UserStreamDao {

    private final static long MAX_LENGTH = 2000; //跟AbstractChatInfo 的 pull_length的保持一致

    private final static String user_stream_key = "%s_user_stream_uid_%d";
	private final static String user_stream_null_key = "%s_user_stream_uid_%d_null";
	private final static String user_stream_incr_key = "%s_user_stream_uid_%d_incr";
	private final static String user_stream_sub_key = "%s_user_stream_uid_%d_type_%d_thirdId_%d";
	private final static String user_stream_sub_null_key = "%s_user_stream_uid_%d_type_%d_thirdId_%d_null";

    private Logger logger = LoggerFactory.getLogger(UserStreamDaoImpl.class);

	private String getTable(long uid){
		int index = (int) uid % chatInfo.user_stream_table_names.size();
		return chatInfo.user_stream_table_names.get(index);
	}

	/**
     * 创建排序字段
	 * method_name: create_sort
	 * params: [streams]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:01
	 **/
	public void create_sort(List<UserStream> streams) throws Exception {
		if(null == streams || streams.size() == 0)
			throw new IllegalStateException("streams is null"); 
		Set<String> keys = Sets.newHashSet();
		List<RedisIncrParam> params = Lists.newArrayList();
		for(UserStream stream : streams){
			String key = String.format(user_stream_incr_key, chatInfo.projectName, stream.getUid());
			keys.add(key);
			RedisIncrParam param = new RedisIncrParam(key);
			param.setAttach(stream);
			params.add(param);
		}
		Map<RedisIncrParam, Long> map = cache.pipIncr(params);
		if(null == map || map.size() ==0 || map.size() != streams.size())
			throw new IllegalStateException("create index error");
		cache.pipExpire(Lists.newArrayList(keys), AbstractChatInfo.redis_overtime_short);
		DecimalFormat decimalFormat = new DecimalFormat("000");
		List<UserStream> nextStreams = Lists.newArrayList();
		for(Entry<RedisIncrParam, Long> entry : map.entrySet()){
			RedisIncrParam param = entry.getKey();
			long num = entry.getValue().longValue();
			long current_time = System.currentTimeMillis();
			UserStream stream = (UserStream)param.getAttach();
			if(num <= 999){
				double index = Double.parseDouble(current_time+"."+decimalFormat.format(num));
				stream.setSort(index);
			}else{
				nextStreams.add(stream);
			}
		}
		if(nextStreams.size() > 0){
			keys = Sets.newHashSet();
			for(UserStream stream : nextStreams){
				String key = String.format(user_stream_incr_key, chatInfo.projectName, stream.getUid());
				keys.add(key);
			}
			cache.multiDelete(Lists.newArrayList(keys));
			this.create_sort(nextStreams);
		} 
	}

	/**
     * 批量添加
	 * method_name: insert
	 * params: [streams]
	 * return: int
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:02
	 **/
	public int insert(List<UserStream> streams) throws DataAccessException {
	    chatInfo.segmentLock.lock(streams);
		int res = 0;
		try {
			String sqlPrev = "insert ignore into %s (uid, `type`, thirdId, mid, authorId, sort) values ";
			String sqlValueFormat = "(%d, %d, %d, %d, %d, %s)";
			Map<String, List<UserStream>> maps = Maps.newHashMap();
			for(UserStream stream : streams){
				String tableName = getTable(stream.getUid());
				List<UserStream> list = maps.get(tableName);
				if(null == list){
					list = Lists.newArrayList();
					maps.put(tableName, list);
				}
				list.add(stream);
			}
			List<String> sqls = Lists.newArrayList();
			List<List<UserStream>> streamList = Lists.newArrayList();
			for(Entry<String, List<UserStream>> entry : maps.entrySet()){
				String tableName = entry.getKey();
				List<UserStream> list = entry.getValue();
				List<String> sqlValueList = Lists.newArrayList();
				for(UserStream stream : list){
				   String sqlValue = String.format(sqlValueFormat, stream.getUid(), 
						   stream.getType(), stream.getThirdId(), stream.getMid(), 
						   stream.getAuthorId(), stream.getSort());
				   sqlValueList.add(sqlValue);
			    }
				String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
				String sql = sqlPrev + sqlValues;
				sql = String.format(sql, tableName);
				sqls.add(sql);
				streamList.add(list);
			}
			long startTime = System.currentTimeMillis();
			int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());

            Set<String> stream_key_set = Sets.newHashSet();
			Set<String> stream_null_key_set = Sets.newHashSet();
			for(UserStream stream : streams){
				long uid = stream.getUid();
				long type = stream.getType();
				long thirdId = stream.getThirdId();
				String key = String.format(user_stream_key, chatInfo.projectName, uid);
				String null_key = String.format(user_stream_null_key, chatInfo.projectName, uid);
				String sub_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
				String sub_null_key = String.format(user_stream_sub_null_key, chatInfo.projectName, uid, type, thirdId);
				stream_key_set.add(key);
				stream_key_set.add(sub_key);
				stream_null_key_set.add(null_key);
				stream_null_key_set.add(sub_null_key);
			}
	        if(streamList.size() != resList.length){
			     cache.multiDelete(Lists.newArrayList(stream_key_set));
			     cache.multiDelete(Lists.newArrayList(stream_null_key_set));
			} else {
				for(int i = 0; i < resList.length; i++){
					int success = resList[i];
					List<UserStream> sub_streams = streamList.get(i);
					int param_num = sub_streams.size();
					Set<String> sub_key_set = Sets.newHashSet();
					Set<String> sub_null_key_set = Sets.newHashSet();
					for(UserStream stream : sub_streams){
						long uid = stream.getUid();
						long type = stream.getType();
						long thirdId = stream.getThirdId();
						String key = String.format(user_stream_key, chatInfo.projectName, uid);
						String null_key = String.format(user_stream_null_key, chatInfo.projectName, uid);
						String sub_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
						String null_sub_key = String.format(user_stream_sub_null_key, chatInfo.projectName, uid, type, thirdId);
						sub_key_set.add(key);
						sub_key_set.add(sub_key);
						sub_null_key_set.add(null_key);
						sub_null_key_set.add(null_sub_key);
					}
					if(success != param_num){
						cache.multiDelete(Lists.newArrayList(sub_key_set));
						cache.multiDelete(Lists.newArrayList(sub_null_key_set));
					} else {
						Map<String, Boolean> exists_map = cache.pipExists(Lists.newArrayList(sub_key_set));
						List<RedisKeySortMemObj> list = Lists.newArrayList();
					    for(UserStream stream : sub_streams){
					    	long uid = stream.getUid();
					    	long type = stream.getType();
							long thirdId = stream.getThirdId();
							String key = String.format(user_stream_key, chatInfo.projectName, uid);
							String sub_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
							if(exists_map.get(key).booleanValue()) {
								RedisKeySortMemObj one = new RedisKeySortMemObj(key, stream, stream.getSort());
					    		list.add(one);
							}
							if(exists_map.get(sub_key).booleanValue()) { 
								RedisKeySortMemObj one = new RedisKeySortMemObj(sub_key, stream, stream.getSort());
					    		list.add(one);
							}
					    }
					    if(list.size() > 0){
					    	cache.pipZaddObject(list);
					    	Set<String> keys = Sets.newHashSet();
					    	for(RedisKeySortMemObj one : list){
				    		    keys.add(one.getKey());
				    	    }
					    	Map<String, Long> card_map = cache.pipZcard(Lists.newArrayList(keys));
					    	List<RedisSortedSetZremrangeParam> params = Lists.newArrayList();
					    	for(Entry<String, Long> entry : card_map.entrySet()){
					    		String key = entry.getKey();
					    		long length = entry.getValue().longValue();
					    		if (length > MAX_LENGTH) {
									int end = (int) (length - MAX_LENGTH - 1);
									RedisSortedSetZremrangeParam param = 
											new RedisSortedSetZremrangeParam(key, 0, end);
									params.add(param);
								}
					    	}
					    	if(params.size() > 0){
					    		cache.pipZremrangeByRank(params);
					    	}
					    }
					}
				}
			}
			if(stream_null_key_set.size() > 0)
			   cache.multiDelete(Lists.newArrayList(stream_null_key_set));
			for(int one : resList) {
				res += one;
			}
			return res;
		} finally {
		    chatInfo.segmentLock.unlock(streams);
			try {
				if(res > 0) {
					long currentTime = System.currentTimeMillis();
					Map<Object, Double> map = Maps.newHashMap();
					for(UserStream stream : streams) {
						long authorId = stream.getAuthorId();
						long uid = stream.getUid();
						long mid = stream.getMid();
						double sort = stream.getSort();
						if(authorId != uid) {
							WillPushItem item = new WillPushItem(uid, mid, sort);
							map.put(item, new Double(currentTime + (1000 * 10)));
						}
					}
					if(map.size() > 0) {
						cache.zaddMultiObject(chatInfo.redis_will_push_queue, map); //添加到预推送队列里
					}
				}
			} catch (Exception e) {
			}
		}
	}
	
	private void _init_handle(long uid) throws Exception {
		String tableName = getTable(uid);
		String key = String.format(user_stream_key, chatInfo.projectName, uid);
		String null_key = String.format(user_stream_null_key, chatInfo.projectName, uid);
		String sql = "select * from %s where uid = %d order by `sort` desc limit %d";
		sql = String.format(sql, tableName, uid, MAX_LENGTH);
		long startTime = System.currentTimeMillis();
		List<UserStream> all_list = this.jdbcTemplate.query(sql, new UserStreamRowMapper());
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        if(null != all_list && all_list.size() > 0){
			Map<Object, Double> map = Maps.newHashMap();
	    	for(UserStream one : all_list){
	    		Double score = new Double(one.getSort());
	    		map.put(one, score);
	    	}
	    	if(map.size() > 0){
	    		cache.zaddMultiObject(key, map);
	    		cache.expire(key, AbstractChatInfo.redis_overtime_long);
	    	}
		}else{
			cache.setString(null_key, AbstractChatInfo.redis_overtime_long, "true");
		}
	}
	
	public void init(long uid, boolean lockSign) throws Exception {
		String key = String.format(user_stream_key, chatInfo.projectName, uid);
		String null_key = String.format(user_stream_null_key, chatInfo.projectName, uid);
		if(!cache.exists(key) && !cache.exists(null_key)){
			if(lockSign) {
				ZkLock lock = null;
				try {
					lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
							chatInfo.projectName, 
							String.format(chatInfo.user_stream_init_lock_key, uid), 
							true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
					if(!cache.exists(key) && !cache.exists(null_key)){
						_init_handle(uid);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (lock != null) lock.release();
				}
			} else {
				_init_handle(uid);
			}
		}
	}
	
	private void _init_sub_handle(long uid, long type, long thirdId) throws Exception {
		String tableName = getTable(uid);
		String sub_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
		String null_sub_key = String.format(user_stream_sub_null_key, chatInfo.projectName, uid, type, thirdId);
		String sql = "select * from %s where uid = %d and type = %d and " +
				     "thirdId = %d order by `sort` desc limit %d";
		sql = String.format(sql, tableName, uid, type, thirdId, MAX_LENGTH);
		long startTime = System.currentTimeMillis();
		List<UserStream> all_list = this.jdbcTemplate.query(sql, new UserStreamRowMapper());
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        if(null != all_list && all_list.size() > 0){
			Map<Object, Double> map = Maps.newHashMap();
	    	for(UserStream one : all_list){
	    		Double score = new Double(one.getSort());
	    		map.put(one, score);
	    	}
	    	if(map.size() > 0){
	    		cache.zaddMultiObject(sub_key, map);
	    		cache.expire(sub_key, AbstractChatInfo.redis_overtime_long);
	    	}
		}else{
			cache.setString(null_sub_key, AbstractChatInfo.redis_overtime_long, "true");
		}
	}

	public void init_sub(long uid, int type, long thirdId, boolean lockSign)
			throws Exception {
		String sub_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
		String null_sub_key = String.format(user_stream_sub_null_key, chatInfo.projectName, uid, type, thirdId);
		if(!cache.exists(sub_key) && !cache.exists(null_sub_key)){ 
			if(lockSign){
				ZkLock lock = null;
				try {
					lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
							chatInfo.projectName, 
							String.format(chatInfo.user_stream_init_lock_key, uid), 
							true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
					if(!cache.exists(sub_key) && !cache.exists(null_sub_key)){
						_init_sub_handle(uid, type, thirdId);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (lock != null) lock.release();
				}
			} else {
				_init_sub_handle(uid, type, thirdId);
			}
		}
	}
	
	public UserStream load(UserStream one) throws DataAccessException {
		long uid = one.getUid();
		String tableName = getTable(uid);
	    String sql = "select * from %s where uid = %d and type = %d and thirdId = %d and mid = %d";
	    long startTime = System.currentTimeMillis();
	    sql = String.format(sql, tableName, uid, one.getType(), one.getThirdId(), one.getMid());
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
	    return this.jdbcTemplate.query(sql, new UserStreamResultSetExtractor());
	}

	public boolean delete(UserStream one) throws DataAccessException {
		boolean res = false;
		long uid = one.getUid();
		int type = one.getType();
		long thirdId = one.getThirdId();
		long mid = one.getMid();
		String tableName = getTable(uid);
		String sql = "delete from %s where uid = ? and type = ? and thirdId = ? and mid = ?";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, uid, type, thirdId, mid);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
		
		if(res){
			String key = String.format(user_stream_key, chatInfo.projectName, uid);
			String sub_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
			if(cache.exists(key)){
				cache.zremObject(key, one);
			}
			if(cache.exists(sub_key)){
				cache.zremObject(sub_key, one);
			}
		}
		return res;
	}

	public List<UserStream> page_list(long uid, long type, long thirdId,
			double cursor, int size) throws DataAccessException {
		List<UserStream> res = Lists.newArrayList();
		double minScore = 0;
	    double maxScore = (cursor == 0) ? Double.MAX_VALUE : cursor;
		String stream_key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
		Set<Object> set = cache.zrevrangeByScoreObject(stream_key, maxScore, minScore, 0, size, null);
	    if(null != set && set.size() > 0){
	    	for(Object one : set){
	    		res.add((UserStream) one);
	    	}
	    } 
		return res;
	}

	public void new_data(List<UserStreamParam> list) throws Exception {
		List<RedisSortedSetParam> params = Lists.newArrayList();
		for(UserStreamParam one : list){
			long uid = one.getUid();
			String key = String.format(user_stream_key, chatInfo.projectName, uid);
			RedisSortedSetParam param = new RedisSortedSetParam();
			param.setKey(key);
			param.setIsasc(false);
			param.setAttach(one);
			param.setOffset(0);
			param.setCount(AbstractChatInfo.pull_length);
			param.setMaxScore(Double.MAX_VALUE);
			param.setMinScore(one.getIndex());
			params.add(param);
		}
		Map<RedisSortedSetParam, Set<Object>> map = cache.pipZrangeByScoreObject(params);
		for(Entry<RedisSortedSetParam, Set<Object>> entry : map.entrySet()){
			RedisSortedSetParam param = entry.getKey();
			Set<Object> set = entry.getValue();
			if(set.size() > 0){
				UserStreamParam stream = (UserStreamParam) param.getAttach();
				List<UserStream> data = Lists.newArrayList();
				for(Object obj : set){
					data.add((UserStream) obj);
				}
				stream.setDataList(data);
			}
		}
	}

	public void one_new_data(List<UserOneStreamParam> list) throws Exception {
//		List<String> sub_keys = Lists.newArrayList();
		List<RedisSortedSetParam> params = Lists.newArrayList();
		for(UserOneStreamParam one : list){
			long uid = one.getUid();
			long type = one.getType();
			long thirdId = one.getThirdId();
			String key = String.format(user_stream_sub_key, chatInfo.projectName, uid, type, thirdId);
//			sub_keys.add(key);
			
			RedisSortedSetParam param = new RedisSortedSetParam();
			param.setKey(key);
			param.setIsasc(false);
			param.setAttach(one);
			param.setOffset(0);
			int count = one.getCount();
			if(count == 0) count = AbstractChatInfo.pull_length;
			param.setCount(count);
			param.setMaxScore(Double.MAX_VALUE);
			param.setMinScore(one.getIndex());
			params.add(param);
		}
//		Map<String, Boolean> sign = cache.pipExists(sub_keys);
//		for(boolean one : sign.values()){
//			if(!one) return;
//		}
		Map<RedisSortedSetParam, Set<Object>> map = cache.pipZrangeByScoreObject(params);
		for(Entry<RedisSortedSetParam, Set<Object>> entry : map.entrySet()){
			RedisSortedSetParam param = entry.getKey();
			Set<Object> set = entry.getValue();
			if(set.size() > 0){
				UserOneStreamParam stream = (UserOneStreamParam) param.getAttach();
				List<UserStream> data = Lists.newArrayList();
				for(Object one : set){
					data.add((UserStream) one);
				}
				stream.setDataList(data);
			}
		}
	}

	class UserStreamRowMapper implements RowMapper<UserStream> {
		@Override
		public UserStream mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			UserStream res = new UserStream();
			res.setMid(rs.getLong("mid"));
			res.setSort(rs.getDouble("sort"));
			res.setThirdId(rs.getLong("thirdId"));
			res.setType(rs.getInt("type"));
			res.setUid(rs.getLong("uid"));
			res.setAuthorId(rs.getLong("authorId"));
			return res;
		}
	}

	class UserStreamResultSetExtractor implements ResultSetExtractor<UserStream>{
		@Override
		public UserStream extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if(rs.next()) {
				UserStream res = new UserStream();
				res.setMid(rs.getLong("mid"));
				res.setSort(rs.getDouble("sort"));
				res.setThirdId(rs.getLong("thirdId"));
				res.setType(rs.getInt("type"));
				res.setUid(rs.getLong("uid"));
				res.setAuthorId(rs.getLong("authorId"));
				return res;
			} else 
				return null;
		}
	}

}
