package com.quakoo.framework.ext.push.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PayloadDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.model.Payload;

public class PayloadDaoImpl extends BaseDao implements PayloadDao, InitializingBean {

	private String object_key;
	
	@Resource
    private DataFieldMaxValueIncrementer payloadMaxValueIncrementer;

    @Override
    public void afterPropertiesSet() throws Exception {
        object_key = pushInfo.projectName + "_payload_object_%d";
    }

    private String getTable(long id){
		int index = (int) id % pushInfo.payload_table_names.size();
		return pushInfo.payload_table_names.get(index);
	}

	@Override
	public Payload insert(Payload payload) throws DataAccessException {
		boolean res = false;
		long id = payloadMaxValueIncrementer.nextLongValue();
		String title = payload.getTitle();
		String content = payload.getContent();
		String extra = JsonUtils.toJson(payload.getExtra());
		long time = System.currentTimeMillis();
		int platform = payload.getPlatform();
		payload.setTime(time);
		String tableName = getTable(id);
		String sql = "insert ignore into %s (id, title, content, extra, `time`, platform) values (?, ?, ?, ?, ?, ?)";
		sql = String.format(sql, tableName);
		int ret = this.jdbcTemplate.update(sql, id, title, content, extra, time, platform);
        res = ret > 0 ? true : false;
        
        if(res){
        	payload.setId(id);
			String key = String.format(object_key, id);
			cache.setObject(key, pushInfo.redis_overtime_long, payload);
			return payload;
		} else {
			return null;
		}
	}

	@Override
	public Payload load(long id) throws DataAccessException {
		String key = String.format(object_key, id);
		Object obj = cache.getObject(key, null);
		if(null != obj){
			return (Payload) obj;
		} else {
			String tableName = getTable(id);
			String sql = "select * from %s where id = %d";
			sql = String.format(sql, tableName, id);
			Payload payload = this.jdbcTemplate
					.query(sql, new PayloadResultSetExtractor());
			if(null != payload){
				cache.setObject(key, pushInfo.redis_overtime_long, payload);
			}
			return payload;
		}
	}

	@Override
	public List<Payload> load(List<Long> ids) throws DataAccessException {
		List<String> object_key_list = Lists.newArrayList();
		Map<Long, String> id_key_map = Maps.newHashMap();
		for(long id : ids){
			String key = String.format(object_key, id);
			object_key_list.add(key);
			id_key_map.put(id, key);
		}
		Map<Long, Payload> res_map = Maps.newHashMap();
		Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
		List<Long> non_ids = Lists.newArrayList();
		for(long id : ids){
			Object obj = redis_map.get(id_key_map.get(id));
			if(null == obj){
				non_ids.add(id);
			} else {
				res_map.put(id, (Payload) obj);
			}
		}
		if(non_ids.size() > 0){
			String sql = "select * from %s where id in (%s)";
			Map<String, List<Long>> maps = Maps.newHashMap();
			for(long id : non_ids){
				String tableName = getTable(id);
				List<Long> list = maps.get(tableName);
				if(null == list){
					list = Lists.newArrayList();
					maps.put(tableName, list);
				}
				list.add(id);
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
				List<Payload> list = this.jdbcTemplate.query(one_sql, 
						new PayloadRowMapper());
				for(Payload payload : list){
					long id = payload.getId();
					res_map.put(id, payload);
					redis_insert_map.put(String.format(object_key, id), payload);
				}
			}
			cache.multiSetObject(redis_insert_map, pushInfo.redis_overtime_long);
		}
		List<Payload> res = Lists.newArrayList();
		for(long id : ids){
			Payload payload = res_map.get(id);
			res.add(payload);
		}
		return res;
	}
	
	class PayloadRowMapper implements RowMapper<Payload> {
		@Override
		public Payload mapRow(ResultSet rs, int rowNum) throws SQLException {
			Payload payload = new Payload();
			payload.setContent(rs.getString("content"));
			Map<String, String> extra = JsonUtils.fromJson(rs.getString("extra"),
					new TypeReference<Map<String, String>>() {});
			payload.setExtra(extra);
		    payload.setId(rs.getLong("id"));
		    payload.setTitle(rs.getString("title"));
		    payload.setTime(rs.getLong("time"));
		    payload.setPlatform(rs.getInt("platform"));
			return payload;
		}
	}
	
	class PayloadResultSetExtractor implements ResultSetExtractor<Payload> {
		@Override
		public Payload extractData(ResultSet rs) throws SQLException,
				DataAccessException {
			if(rs.next()){
				Payload payload = new Payload();
				payload.setContent(rs.getString("content"));
				Map<String, String> extra = JsonUtils.fromJson(rs.getString("extra"),
						new TypeReference<Map<String, String>>() {});
				payload.setExtra(extra);
			    payload.setId(rs.getLong("id"));
			    payload.setTitle(rs.getString("title"));
			    payload.setTime(rs.getLong("time"));
                payload.setPlatform(rs.getInt("platform"));
				return payload;
			} else
				return null;
		}
	}
}
