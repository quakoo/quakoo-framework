package com.quakoo.framework.ext.chat.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.model.Message;

public class MessageDaoImpl extends BaseDaoHandle implements MessageDao {

    private static final String object_key = "%s_message_object_%d";

    private static final String temp_object_key = "%s_message_object_temp_uid_%d_clientId_%s";

    private Logger logger = LoggerFactory.getLogger(MessageDaoImpl.class);

    @Resource
    private DataFieldMaxValueIncrementer messageMaxValueIncrementer;

    private String getTable(long id){
        int index = (int) id % chatInfo.message_table_names.size();
        return chatInfo.message_table_names.get(index);
    }

    @Override
    public List<Long> getMessageIds(int size) throws DataAccessException {
        List<Long> res = Lists.newArrayList();
        for(int i = 0; i < size; i++) {
            long id = messageMaxValueIncrementer.nextLongValue();
            res.add(id);
        }
        return res;
    }

    @Override
    public List<Message> insert(List<Message> messages) throws DataAccessException {
        List<String> tempKeys = Lists.newArrayList();
        for(Message message : messages) {
            String tempKey = String.format(temp_object_key, chatInfo.projectName,
                    message.getAuthorId(), message.getClientId());
            tempKeys.add(tempKey);
        }
        Map<String, Object> map = cache.multiGetObject(tempKeys, null);
        List<String> existsMsgInfos = Lists.newArrayList();
        for(Object one : map.values()) {
            Message message = (Message) one;
            existsMsgInfos.add(String.format("%d_%s", message.getAuthorId(), message.getClientId()));
        }
        for(Iterator<Message> it = messages.iterator(); it.hasNext();) {
            Message message = it.next();
            String msgInfo = String.format("%d_%s", message.getAuthorId(), message.getClientId());
            if(existsMsgInfos.contains(msgInfo)) it.remove();
        }
        Map<String, List<Message>> tableMessagesMap = Maps.newHashMap();
        for(Message message : messages) {
            String tableName = getTable(message.getId());
            List<Message> messageList = tableMessagesMap.get(tableName);
            if(null == messageList) {
                messageList = Lists.newArrayList();
                tableMessagesMap.put(tableName, messageList);
            }
            messageList.add(message);
        }
        String sqlFormat = "insert ignore into %s (id, authorId, clientId, `type`, thirdId, content, `time`) values (?, ?, ?, ?, ?, ?, ?)";
        List<Message> successMessages = Lists.newArrayList();
        for(Entry<String, List<Message>> entry : tableMessagesMap.entrySet()) {
            String tableName = entry.getKey();
            final List<Message> messageList = entry.getValue();
            String sql = String.format(sqlFormat, tableName);
            long startTime = System.currentTimeMillis();
            int[] res = this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Message one =  messageList.get(i);
                    ps.setLong(1, one.getId());
                    ps.setLong(2, one.getAuthorId());
                    ps.setString(3, one.getClientId());
                    ps.setInt(4, one.getType());
                    ps.setLong(5, one.getThirdId());
                    ps.setString(6, one.getContent());
                    ps.setLong(7, System.currentTimeMillis());
                }
                @Override
                public int getBatchSize() {
                    return messageList.size();
                }
            });
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql
                    + " , message : " + messageList.toString());
            for(int i = 0; i < res.length; i++) {
                int ret = res[i];
                Message message = messageList.get(i);
                if(ret > 0) successMessages.add(message);
            }
        }
        if(successMessages.size() > 0) {
            Map<String, Object> redisMap = Maps.newHashMap();
            for(Message message : successMessages) {
                String key = String.format(object_key, chatInfo.projectName, message.getId());
                String checkKey = String.format(temp_object_key, chatInfo.projectName,
                        message.getAuthorId(), message.getClientId());
                redisMap.put(key, message);
                redisMap.put(checkKey, message);
            }
            cache.multiSetObject(redisMap, AbstractChatInfo.redis_overtime_long);
        }
        return successMessages;
    }

    @Override
    public Message insert(Message message) throws DataAccessException {
        boolean res = false;
        long id = messageMaxValueIncrementer.nextLongValue();
        long authorId = message.getAuthorId();
        String clientId = message.getClientId();
        int type = message.getType();
        long thirdId = message.getThirdId();
        String content = message.getContent();
        long time = System.currentTimeMillis();

        String tempKey = String.format(temp_object_key, chatInfo.projectName, authorId, clientId);
        boolean exists = cache.exists(tempKey);
        if(exists) return null;

        String tableName = getTable(id);
        String sql = "insert ignore into %s (id, authorId, clientId, `type`, thirdId, content, `time`) values (?, ?, ?, ?, ?, ?, ?)";
        sql = String.format(sql, tableName);
        long startTime = System.currentTimeMillis();
        int ret = this.jdbcTemplate.update(sql, id, authorId, clientId, type, thirdId, content, time);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
        if(res){
            message.setId(id);
            message.setContent(content);
            String key = String.format(object_key, chatInfo.projectName, id);
//			cache.setObject(key, AbstractChatInfo.redis_overtime_long, message);
//
            String checkKey = String.format(temp_object_key, chatInfo.projectName, authorId, clientId);
//            cache.setObject(key, AbstractChatInfo.redis_overtime_long, message);

            Map<String, Object> redisMap = Maps.newHashMap();
            redisMap.put(key, message);
            redisMap.put(checkKey, message);
            cache.multiSetObject(redisMap, AbstractChatInfo.redis_overtime_long);

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
    public Message tempLoad(long authorId, String clientId) throws DataAccessException {
        String key = String.format(temp_object_key, chatInfo.projectName, authorId, clientId);
        Object obj = cache.getObject(key, null);
        if(null == obj) return null;
        else return (Message) obj;
//        if(null != obj){
//            return (Message) obj;
//        } else {
//            String tableName = getTable(authorId);
//            String sql = "select * from %s where authorId = %d and clientId = '%s'";
//            sql = String.format(sql, tableName, authorId, clientId);
//            long startTime = System.currentTimeMillis();
//            Message message = this.jdbcTemplate.query(sql, new MessageResultSetExtractor());
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//            if(null != message){
//                cache.setObject(key, AbstractChatInfo.redis_overtime_long, message);
//            }
//            return message;
//        }
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
            message.setThirdId(rs.getLong("thirdId"));
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
                message.setThirdId(rs.getLong("thirdId"));
                message.setContent(rs.getString("content"));
                message.setTime(rs.getLong("time"));
                return message;
            } else
                return null;
        }
    }

}
