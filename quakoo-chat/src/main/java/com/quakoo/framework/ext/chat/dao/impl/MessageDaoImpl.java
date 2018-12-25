package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.model.Message;

public class MessageDaoImpl extends BaseDaoHandle implements MessageDao {

	private static final String object_key = "%s_message_object_%d";

	private Logger logger = LoggerFactory.getLogger(MessageDaoImpl.class);
	
    @Resource
    private DataFieldMaxValueIncrementer messageMaxValueIncrementer;
    
    private String getTable(long id){
		int index = (int) id % chatInfo.message_table_names.size();
		return chatInfo.message_table_names.get(index);
	}

    @Override
	public Message insert(Message message) throws DataAccessException {
		boolean res = false;
		long id = messageMaxValueIncrementer.nextLongValue();
		long authorId = message.getAuthorId();
		String clientId = message.getClientId();
		int type = message.getType();
		String content = message.getContent();
		long time = System.currentTimeMillis();
		String tableName = getTable(id);
		String sql = "insert ignore into %s (id, authorId, clientId, `type`, content, `time`) values (?, ?, ?, ?, ?, ?)";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, id, authorId, clientId, type, content, time);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

        res = ret > 0 ? true : false;
        
        if(res){
        	message.setId(id);
        	message.setContent(content);
			String key = String.format(object_key, chatInfo.projectName, id);
			cache.setObject(key, AbstractChatInfo.redis_overtime_long, message);
			return message;
		} else {
			return null;
		}
	}

    @Override
    public boolean updateContent(long id, String content) throws DataAccessException {
        boolean res = false;
        String tableName = getTable(id);
        String sql = "update %s set `content` = ? where id = ?";
        sql = String.format(sql, tableName);
        long startTime = System.currentTimeMillis();
        int ret = this.jdbcTemplate.update(sql, content, id);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
        if(res){
            String key = String.format(object_key, chatInfo.projectName, id);
            cache.delete(key);
        }
        return res;
    }

    @Override
	public Message load(long id) throws DataAccessException {
		String key = String.format(object_key, chatInfo.projectName, id);
		Object obj = cache.getObject(key, null);
		if(null != obj){
			return (Message) obj;
		} else {
			String tableName = getTable(id);
			String sql = "select * from %s where id = %d";
			sql = String.format(sql, tableName, id);
            long startTime = System.currentTimeMillis();
			Message message = this.jdbcTemplate
					.query(sql, new MessageResultSetExtractor());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

            if(null != message){
				cache.setObject(key, AbstractChatInfo.redis_overtime_long, message);
			}
			return message;
		}
	}

    @Override
	public List<Message> load(List<Long> ids) throws DataAccessException {
		List<String> object_key_list = Lists.newArrayList();
		Map<Long, String> id_key_map = Maps.newHashMap();
		for(long id : ids){
			String key = String.format(object_key, chatInfo.projectName, id);
			object_key_list.add(key);
			id_key_map.put(id, key);
		}
		Map<Long, Message> res_map = Maps.newHashMap();
		Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
		List<Long> non_ids = Lists.newArrayList();
		for(long id : ids){
			Object obj = redis_map.get(id_key_map.get(id));
			if(null == obj){
				non_ids.add(id);
			} else {
				res_map.put(id, (Message) obj);
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
			    long startTime = System.currentTimeMillis();
				List<Message> list = this.jdbcTemplate.query(one_sql, 
						new MessageRowMapper());
                logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + one_sql);

                for(Message message : list){
					long id = message.getId();
					res_map.put(id, message);
					redis_insert_map.put(String.format(object_key, chatInfo.projectName, id), message);
				}
			}
			cache.multiSetObject(redis_insert_map, AbstractChatInfo.redis_overtime_long);
		}
		List<Message> res = Lists.newArrayList();
		for(long id : ids){
			Message message = res_map.get(id);
			res.add(message);
		}
		return res;
	}
	
	class MessageRowMapper implements RowMapper<Message> {
		@Override
		public Message mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			Message message = new Message();
			message.setAuthorId(rs.getLong("authorId"));
			message.setClientId(rs.getString("clientId"));
			message.setId(rs.getLong("id"));
			message.setType(rs.getInt("type"));
			message.setContent(rs.getString("content"));
			message.setTime(rs.getLong("time"));
			return message;
		}
	}
	
	class MessageResultSetExtractor implements ResultSetExtractor<Message> {
		@Override
		public Message extractData(ResultSet rs) throws SQLException,
				DataAccessException {
			if(rs.next()){
				Message message = new Message();
				message.setAuthorId(rs.getLong("authorId"));
				message.setClientId(rs.getString("clientId"));
				message.setId(rs.getLong("id"));
				message.setType(rs.getInt("type"));
				message.setContent(rs.getString("content"));
				message.setTime(rs.getLong("time"));
				return message;
			} else
				return null;
		}
	}
}
