package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.redis.RedisSortedSetZremrangeParam;
import com.quakoo.baseFramework.redis.RedisSortData.RedisKeySortMemObj;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.UserPromptDao;
import com.quakoo.framework.ext.chat.model.UserPrompt;

public class UserPromptDaoImpl extends BaseDaoHandle implements UserPromptDao {

    private final static String user_prompt_queue_key = "%s_user_prompt_uid_%d_queue";
	private final static String user_prompt_queue_null_key = "%s_user_prompt_uid_%d_queue_null";
	
	private final static int max_queue_num = 100;

	private Logger logger = LoggerFactory.getLogger(UserPromptDaoImpl.class);
	
	private String getTable(long uid){
		long index = uid % chatInfo.user_prompt_table_names.size();
		return chatInfo.user_prompt_table_names.get((int) index);
	}
	
	public int insert(List<UserPrompt> userPrompts) throws DataAccessException {
		String sqlPrev = "insert ignore into %s (uid, `type`, thirdId, sort) values ";
		String sqlValueFormat = "(%d, %d, %d, %s)";
		Map<String, List<UserPrompt>> maps = Maps.newHashMap();
		for(UserPrompt prompt : userPrompts){
			String tableName = getTable(prompt.getUid());
			List<UserPrompt> list = maps.get(tableName);
			if(null == list){
				list = Lists.newArrayList();
				maps.put(tableName, list);
			}
			list.add(prompt);
		}
		List<String> sqls = Lists.newArrayList();
		List<List<UserPrompt>> promptList = Lists.newArrayList();
		for(Entry<String, List<UserPrompt>> entry : maps.entrySet()){
			String tableName = entry.getKey();
			List<UserPrompt> list = entry.getValue();
			List<String> sqlValueList = Lists.newArrayList();
			for(UserPrompt prompt : list){
			   String sqlValue = String.format(sqlValueFormat, prompt.getUid(), 
					   prompt.getType(), prompt.getThirdId(), prompt.getSort());
			   sqlValueList.add(sqlValue);
		    }
			String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
			String sql = sqlPrev + sqlValues;
			sql = String.format(sql, tableName);
			sqls.add(sql);
			promptList.add(list);
		}
		long startTime = System.currentTimeMillis();
		int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sqls.toString());

        Set<String> prompt_key_set = Sets.newHashSet();
		Set<String> prompt_null_key_set = Sets.newHashSet();
		for(UserPrompt prompt : userPrompts){
			long uid = prompt.getUid();
			String key = String.format(user_prompt_queue_key, chatInfo.projectName, uid);
			String null_key = String.format(user_prompt_queue_null_key, chatInfo.projectName, uid);
			prompt_key_set.add(key);
			prompt_null_key_set.add(null_key);
		}
		if(promptList.size() != resList.length){
		    cache.multiDelete(Lists.newArrayList(prompt_key_set));
		    cache.multiDelete(Lists.newArrayList(prompt_null_key_set));
		} else {
			for(int i = 0; i < resList.length; i++){
				int success = resList[i];
				List<UserPrompt> sub_prompts = promptList.get(i);
				int param_num = sub_prompts.size();
				Set<String> sub_key_set = Sets.newHashSet();
				Set<String> sub_null_key_set = Sets.newHashSet();
				for(UserPrompt prompt : sub_prompts){
					long uid = prompt.getUid();
					String key = String.format(user_prompt_queue_key, chatInfo.projectName, uid);
					String null_key = String.format(user_prompt_queue_null_key, chatInfo.projectName, uid);
					sub_key_set.add(key);
					sub_null_key_set.add(null_key);
				}
				if(success != param_num){
					cache.multiDelete(Lists.newArrayList(sub_key_set));
					cache.multiDelete(Lists.newArrayList(sub_null_key_set));
				} else {
					Map<String, Boolean> exists_map = cache.pipExists(Lists.newArrayList(sub_key_set));
					List<RedisKeySortMemObj> list = Lists.newArrayList();
				    for(UserPrompt prompt : sub_prompts){
				    	long uid = prompt.getUid();
						String key = String.format(user_prompt_queue_key, chatInfo.projectName, uid);
						if(exists_map.get(key).booleanValue()) {
							RedisKeySortMemObj one = new RedisKeySortMemObj(key, prompt, prompt.getSort());
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
				    		if (length > max_queue_num) {
								int end = (int) (length - max_queue_num - 1);
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
		if(prompt_null_key_set.size() > 0) 
			cache.multiDelete(Lists.newArrayList(prompt_null_key_set));
		int res = 0;
		for(int one : resList) {
			res += one;
		}
		return res;
	}

	public boolean insert(UserPrompt userPrompt) throws DataAccessException {
		long uid = userPrompt.getUid();
		double sort = userPrompt.getSort();
		int type = userPrompt.getType();
		long thirdId = userPrompt.getThirdId();
		String tableName = this.getTable(uid);
		boolean res = false;
		String sql = "insert into %s (uid, `type`, thirdId, sort) values (?, ?, ?, ?)";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, uid, type, thirdId, sort);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        res = ret > 0 ? true : false;
		if(res){
			String queue_null_key = String.format(user_prompt_queue_null_key, chatInfo.projectName, uid);
			cache.delete(queue_null_key);
			String queue_key = String.format(user_prompt_queue_key, chatInfo.projectName, uid);
			if(cache.exists(queue_key)){
				cache.zaddObject(queue_key, sort, userPrompt);
				long length = cache.zcard(queue_key);
				if(length > max_queue_num){
					cache.zremrangeByRank(queue_key, 0, (int)(length - max_queue_num -1));
				}
			}
		}
		return res;
	}
	
	private void init(long uid) throws Exception {
		String key = String.format(user_prompt_queue_key, chatInfo.projectName, uid);
		String null_key = String.format(user_prompt_queue_null_key, chatInfo.projectName, uid);
		if(!cache.exists(key) && !cache.exists(null_key)){
			String tableName = getTable(uid);
			String sql = "select * from %s where uid = %d order by `sort` desc limit %d";
			sql = String.format(sql, tableName, uid, max_queue_num);
			long startTime = System.currentTimeMillis();
			List<UserPrompt> all_list = this.jdbcTemplate.query(sql, new UserPromptRowMapper());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

            if(null != all_list && all_list.size() > 0){
				Map<Object, Double> map = Maps.newHashMap();
				for(UserPrompt one : all_list){
		    		Double score = new Double(one.getSort());
		    		map.put(one, score);
		    	}
				if(map.size() > 0){
					cache.zaddMultiObject(key, map);
		    		cache.expire(key, AbstractChatInfo.redis_overtime_long);
		    	}
			} else {
				cache.setString(null_key, AbstractChatInfo.redis_overtime_long, "true");
			}
		}
	}

	public List<UserPrompt> new_data(long uid, double index) throws Exception {
		this.init(uid);
		String queue_key = String.format(user_prompt_queue_key, chatInfo.projectName, uid);
		String queue_null_key = String.format(user_prompt_queue_null_key, chatInfo.projectName, uid);
		if(cache.exists(queue_null_key)) 
			return Lists.newArrayList();
		if(cache.exists(queue_key)){
			List<UserPrompt> res = Lists.newArrayList();
			Set<Object> set = cache.zrevrangeByScoreObject(queue_key, Double.MAX_VALUE, index, null);
			if(null != set && set.size() > 0){
			    for(Object one : set){
			   		res.add((UserPrompt) one);
			   	}
			}
			return res;
		}
		return Lists.newArrayList();
	}
	
	class UserPromptRowMapper implements RowMapper<UserPrompt> {
		@Override
		public UserPrompt mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			UserPrompt res = new UserPrompt();
			res.setSort(rs.getDouble("sort"));
			res.setThirdId(rs.getLong("thirdId"));
			res.setType(rs.getInt("type"));
			res.setUid(rs.getLong("uid"));
			res.setNum(rs.getLong("num"));
			return res;
		}
	}

}
