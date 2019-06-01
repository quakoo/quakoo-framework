package com.quakoo.framework.ext.push.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushMsgHandleAllQueueDao;
import com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推送所有用户DAO
 * class_name: PushMsgHandleAllQueueDaoImpl
 * package: com.quakoo.framework.ext.push.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:35
 **/
public class PushMsgHandleAllQueueDaoImpl extends BaseDao implements PushMsgHandleAllQueueDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(PushMsgHandleAllQueueDaoImpl.class);

    private String push_msg_handle_all_queue_key;
    private String push_msg_handle_all_queue_null_key;

    private JedisX cache;


    @Override
    public void afterPropertiesSet() throws Exception {
        push_msg_handle_all_queue_key = pushInfo.projectName + "_push_msg_handle_all_queue";
        push_msg_handle_all_queue_null_key = pushInfo.projectName  + "_push_msg_handle_all_queue_null";
        cache = new JedisX(pushInfo.redisInfo, pushInfo.redisConfig, 2000);
    }

    /**
     * 插入推送所有用户的通知队列
     * method_name: insert
     * params: [one]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:35
     **/
    @Override
    public boolean insert(final PushMsgHandleAllQueue one) throws DataAccessException {
        boolean res = false;
        final String sql = "insert into push_msg_handle_all_queue (pushMsgId, title, content, extra, platform, `time`) values (?,?,?,?,?,?)";
        PreparedStatementCreator preparedStatementCreator = new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con)
                    throws SQLException {
                PreparedStatement ps = con.prepareStatement(sql,
                        new String[] { "id" });
                ps.setLong(1, one.getPushMsgId());
                ps.setString(2, one.getTitle());
                ps.setString(3, one.getContent());
                String extra = JsonUtils.toJson(one.getExtra());
                ps.setString(4, extra);
                ps.setInt(5, one.getPlatform());
                ps.setLong(6, one.getTime());
                return ps;
            }
        };
        KeyHolder key = new GeneratedKeyHolder();
        long startTime = System.currentTimeMillis();
        int ret = this.jdbcTemplate.update(preparedStatementCreator, key);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        res = ret > 0 ? true : false;
        if(res) {
            long id = key.getKey().longValue();
            one.setId(id);
            cache.delete(push_msg_handle_all_queue_null_key);
            if(cache.exists(push_msg_handle_all_queue_key)){
                cache.zaddObject(push_msg_handle_all_queue_key, new Double(id), one);
            }
        }
        return res;
    }

    /**
     * 初始化队列
     * method_name: init
     * params: []
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:36
     **/
    private void init() {
        if(!cache.exists(push_msg_handle_all_queue_null_key) && !cache.exists(push_msg_handle_all_queue_key)) {
            ZkLock lock = null;
            try {
                lock = ZkLock.getAndLock(pushInfo.pushLockZkAddress,
                        pushInfo.projectName, push_msg_handle_all_queue_key + pushInfo.lock_suffix,
                        true, pushInfo.session_timout, pushInfo.lock_timeout);
                if(!cache.exists(push_msg_handle_all_queue_null_key) && !cache.exists(push_msg_handle_all_queue_key)) {
                    String sql = "select * from push_msg_handle_all_queue order by id asc";
                    long startTime = System.currentTimeMillis();
                    List<PushMsgHandleAllQueue> all_list = this.jdbcTemplate.query(sql,
                            new PushMsgHandleAllQueueRowMapper());
                    logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
                    if(null != all_list && all_list.size() > 0){
                        Map<Object, Double> map = Maps.newHashMap();
                        for(PushMsgHandleAllQueue one : all_list){
                            Double score = new Double(one.getId());
                            map.put(one, score);
                        }
                        if(map.size() > 0){
                            cache.zaddMultiObject(push_msg_handle_all_queue_key, map);
                            cache.expire(push_msg_handle_all_queue_key, pushInfo.redis_overtime_long);
                        }
                    }else{
                        cache.setString(push_msg_handle_all_queue_null_key, pushInfo.redis_overtime_long, "true");
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
     * 获取推送所有用户的队列
     * method_name: getList
     * params: [minId, size]
     * return: java.util.List<com.quakoo.framework.ext.push.model.PushMsgHandleAllQueue>
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:36
     **/
    @Override
    public List<PushMsgHandleAllQueue> getList(long minId, int size)
            throws DataAccessException {
        this.init();
        if(cache.exists(push_msg_handle_all_queue_null_key))
            return Lists.newArrayList();
        if(cache.exists(push_msg_handle_all_queue_key)){
            List<PushMsgHandleAllQueue> res = Lists.newArrayList();
            Set<Object> set = cache.zrangeByScoreObject(push_msg_handle_all_queue_key, new Double(minId) + 0.1,
                    Double.MAX_VALUE, 0, size, null);
            if(null != set && set.size() > 0){
                for(Object one : set){
                    res.add((PushMsgHandleAllQueue) one);
                }
            }
            return res;
        }
        return Lists.newArrayList();
    }

    @Override
    public PushMsgHandleAllQueue load(long id) throws DataAccessException {
        this.init();
        if(cache.exists(push_msg_handle_all_queue_null_key)) {
            return null;
        }
        if(cache.exists(push_msg_handle_all_queue_key)){
            Set<Object> set = cache.zrangeByScoreObject(push_msg_handle_all_queue_key, new Double(id - 1) + 0.1,
                    Double.MAX_VALUE, 0, 1, null);
            if(null != set && set.size() > 0){
                return (PushMsgHandleAllQueue) set.iterator().next() ;
            }
        }
        return null;
    }

    class PushMsgHandleAllQueueRowMapper implements RowMapper<PushMsgHandleAllQueue> {
        @Override
        public PushMsgHandleAllQueue mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            PushMsgHandleAllQueue res = new PushMsgHandleAllQueue();
            res.setId(rs.getLong("id"));
            res.setPushMsgId(rs.getLong("pushMsgId"));
            res.setTitle(rs.getString("title"));
            res.setContent(rs.getString("content"));
            Map<String, String> extra = JsonUtils.fromJson(rs.getString("extra"),
                    new TypeReference<Map<String, String>>() {});
            res.setExtra(extra);
            res.setPlatform(rs.getInt("platform"));
            res.setTime(rs.getLong("time"));
            return res;
        }
    }

}
