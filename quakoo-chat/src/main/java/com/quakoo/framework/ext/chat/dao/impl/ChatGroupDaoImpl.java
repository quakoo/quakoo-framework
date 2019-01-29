package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.ChatGroupDao;
import com.quakoo.framework.ext.chat.model.ChatGroup;

/**
 * 群组DAO
 * class_name: ChatGroupDaoImpl
 * package: com.quakoo.framework.ext.chat.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 16:50
 **/
public class ChatGroupDaoImpl extends BaseDaoHandle implements ChatGroupDao {

    private Logger logger = LoggerFactory.getLogger(ChatGroupDaoImpl.class);

	private static final String object_key = "%s_chat_group_object_%d";
    
    @Resource
    private DataFieldMaxValueIncrementer chatGroupMaxValueIncrementer;

    /**
     * 获取表名(根据群组id获取表名)
     * method_name: getTable
     * params: [id]
     * return: java.lang.String
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 16:51
     **/
    private String getTable(long id){
		int index = (int) id % chatInfo.chat_group_table_names.size();
		return chatInfo.chat_group_table_names.get(index);
	}
    
    @Override
	public ChatGroup insert(ChatGroup chatGroup) throws DataAccessException {
		boolean res = false;
		long id = chatGroupMaxValueIncrementer.nextLongValue();
		String name = chatGroup.getName();
		String uids = chatGroup.getUids();
        String icon = chatGroup.getIcon();
        int check = chatGroup.getCheck();
        String notice = chatGroup.getNotice();
		String tableName = getTable(id);
		String sql = "insert ignore into %s (id, `name`, uids, `icon`, `check`, `notice`) values (?, ?, ?, ?, ?, ?)";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, id, name, uids, icon, check, notice);
		logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
		if(res){
			chatGroup.setId(id);
			String key = String.format(object_key, chatInfo.projectName, id);
			cache.setObject(key, AbstractChatInfo.redis_overtime_long, chatGroup);
			return chatGroup;
		} else {
			return null;
		}
	}

    @Override
	public ChatGroup load(long id) throws DataAccessException {
		String key = String.format(object_key, chatInfo.projectName, id);
		Object obj = cache.getObject(key, null);
		if(null != obj){
			return (ChatGroup) obj;
		} else {
			String tableName = getTable(id);
			String sql = "select * from %s where id = %d";
			long startTime = System.currentTimeMillis();
			sql = String.format(sql, tableName, id);
			ChatGroup chatGroup = this.jdbcTemplate
					.query(sql, new ChatGroupResultSetExtractor());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);

            if(null != chatGroup){
				cache.setObject(key, AbstractChatInfo.redis_overtime_long, chatGroup);
			}
			return chatGroup;
		}
	}

    @Override
	public List<ChatGroup> load(List<Long> ids) throws DataAccessException {
		List<String> object_key_list = Lists.newArrayList();
		Map<Long, String> id_key_map = Maps.newHashMap();
		for(long id : ids){
			String key = String.format(object_key, chatInfo.projectName, id);
			object_key_list.add(key);
			id_key_map.put(id, key);
		}
		Map<Long, ChatGroup> res_map = Maps.newHashMap();
		Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
		List<Long> non_ids = Lists.newArrayList();
		for(long id : ids){
			Object obj = redis_map.get(id_key_map.get(id));
			if(null == obj){
				non_ids.add(id);
			} else {
				res_map.put(id, (ChatGroup) obj);
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
				List<ChatGroup> list = this.jdbcTemplate.query(one_sql, 
						new ChatGroupRowMapper());
                logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + one_sql);

                for(ChatGroup chatGroup : list){
					long id = chatGroup.getId();
					res_map.put(id, chatGroup);
					redis_insert_map.put(String.format(object_key, chatInfo.projectName, id), chatGroup);
				}
			}
			cache.multiSetObject(redis_insert_map, AbstractChatInfo.redis_overtime_long);
		}
		List<ChatGroup> res = Lists.newArrayList();
		for(long id : ids){
			ChatGroup chatGroup = res_map.get(id);
			res.add(chatGroup);
		}
		return res;
	}

    @Override
	public boolean update(ChatGroup chatGroup) throws DataAccessException {
		boolean res = false;
		long id = chatGroup.getId();
		String name = chatGroup.getName();
		String uids = chatGroup.getUids();
		String icon = chatGroup.getIcon();
		int check = chatGroup.getCheck();
		String notice = chatGroup.getNotice();
		String tableName = getTable(id);
		String sql = "update %s set `name` = ?, uids = ?, `icon` = ?, `check` = ?, `notice` = ? where id = ?";
		sql = String.format(sql, tableName);
		long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, name, uids, icon, check, notice, id);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
		if(res){
			String key = String.format(object_key, chatInfo.projectName, id);
			cache.setObject(key, AbstractChatInfo.redis_overtime_long, chatGroup);
		}
		return res;
	}
	
	class ChatGroupResultSetExtractor implements ResultSetExtractor<ChatGroup> {
		@Override
		public ChatGroup extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if(rs.next()){
				ChatGroup chatGroup = new ChatGroup();
				chatGroup.setId(rs.getLong("id"));
				chatGroup.setName(rs.getString("name"));
				chatGroup.setUids(rs.getString("uids"));
				chatGroup.setIcon(rs.getString("icon"));
				chatGroup.setCheck(rs.getInt("check"));
				chatGroup.setNotice(rs.getString("notice"));
				return chatGroup;
			} else
				return null;
		}
	}
	
	class ChatGroupRowMapper implements RowMapper<ChatGroup> {
		@Override
		public ChatGroup mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			ChatGroup chatGroup = new ChatGroup();
			chatGroup.setId(rs.getLong("id"));
			chatGroup.setName(rs.getString("name"));
			chatGroup.setUids(rs.getString("uids"));
			chatGroup.setIcon(rs.getString("icon"));
            chatGroup.setCheck(rs.getInt("check"));
            chatGroup.setNotice(rs.getString("notice"));
			return chatGroup;
		}
	}

}
