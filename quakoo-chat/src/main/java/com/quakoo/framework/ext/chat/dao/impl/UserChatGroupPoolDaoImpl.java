package com.quakoo.framework.ext.chat.dao.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.bean.UserChatGroup;
import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
import com.quakoo.framework.ext.chat.dao.UserChatGroupPoolDao;
import com.quakoo.framework.ext.chat.model.UserChatGroupPool;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import com.quakoo.baseFramework.redis.RedisSortData.RedisKeySortMemObj;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserChatGroupPoolDaoImpl extends BaseDaoHandle implements UserChatGroupPoolDao, InitializingBean {

    private static final String user_chat_group_pool_object_key = "%s_user_chat_group_pool_object_uid_%d_cgid_%d";
    private static final String user_chat_group_list_key = "%s_user_chat_group_list_uid_%d";
    private static final String user_chat_group_list_null_key = "%s_user_chat_group_list_uid_%d_null";

    private Logger logger = LoggerFactory.getLogger(UserChatGroupPoolDaoImpl.class);

    private JedisX cache;

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(chatInfo.redisInfo, chatInfo.redisConfig, 2000);
    }

    private String getTable() {
        return "user_chat_group_pool";
    }

    @Override
    public UserChatGroupPool load(UserChatGroupPool pool) throws DataAccessException {
        String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
        Object obj = cache.getObject(key, null);
        if(obj != null) {
            return (UserChatGroupPool) obj;
        } else {
            String sql = "select * from %s where uid = %d and cgid = %d";
            sql = String.format(sql, getTable(), pool.getUid(), pool.getCgid());
            UserChatGroupPool res = this.jdbcTemplate.query(sql, new UserChatGroupPoolResultSetExtractor());
            if(res != null) {
                cache.setObject(key, AbstractChatInfo.redis_overtime_long, res);
            }
            return res;
        }
    }

    @Override
    public List<UserChatGroupPool> load(List<UserChatGroupPool> pools) throws DataAccessException {
        List<UserChatGroupPool> res = Lists.newArrayList();
        List<String> redisList = Lists.newArrayList();
        for(UserChatGroupPool pool : pools) {
            String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
            redisList.add(key);
        }
        Map<String, Object> map = cache.multiGetObject(redisList, null);
        List<UserChatGroupPool> mysqls = Lists.newArrayList();
        for(UserChatGroupPool pool : pools) {
            String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
            Object obj = map.get(key);
            if(obj == null) mysqls.add(pool);
        }
        if(mysqls.size() > 0) {
            String sqlPrev = "select * from %s where ";
            String sqlValueFormat = "(uid = %d and cgid = %d)";
            List<String> sqlValueList = Lists.newArrayList();
            for (UserChatGroupPool pool : mysqls) {
                String sqlValue = String.format(sqlValueFormat, pool.getUid(), pool.getCgid());
                sqlValueList.add(sqlValue);
            }
            String sqlValues = StringUtils.join(sqlValueList.toArray(), " or ");
            String sql = sqlPrev + sqlValues;
            sql = String.format(sql, getTable());
            List<UserChatGroupPool> list = this.jdbcTemplate.query(sql, new UserChatGroupPoolRowMapper());
            if(list.size() > 0) {
                Map<String, Object> redisMap = Maps.newHashMap();
                for(UserChatGroupPool pool : list) {
                    String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
                    map.put(key, pool);
                    redisMap.put(key, pool);
                }
                cache.multiSetObject(redisMap, AbstractChatInfo.redis_overtime_long);
            }
        }
        for(UserChatGroupPool pool : pools) {
            String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
            Object obj = map.get(key);
            if(obj != null) res.add((UserChatGroupPool) obj);
            else res.add(null);
        }
        return res;
    }

    @Override
    public int insert(List<UserChatGroupPool> pools) throws DataAccessException {
        long time = System.currentTimeMillis();
        Map<Long, List<UserChatGroupPool>> map = Maps.newHashMap();
        for (UserChatGroupPool pool : pools) {
            pool.setCtime(time);
            pool.setUtime(time);
            List<UserChatGroupPool> list = map.get(pool.getUid());
            if(list == null) {
                list = Lists.newArrayList();
                map.put(pool.getUid(), list);
            }
            list.add(pool);
        }
        String sqlPrev = "insert ignore into %s (uid, cgid, `type`, `status`, inviteUid, ctime, utime) values ";
        String sqlValueFormat = "(%d, %d, %d, %d, %d, %d, %d)";
        List<String> sqlValueList = Lists.newArrayList();
        for (UserChatGroupPool pool : pools) {
            String sqlValue = String.format(sqlValueFormat, pool.getUid(), pool.getCgid(), pool.getType(), pool.getStatus(), pool.getInviteUid(), pool.getCtime(), pool.getUtime());
            sqlValueList.add(sqlValue);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), ",");
        String sql = sqlPrev + sqlValues;
        sql = String.format(sql, getTable());
        long startTime = System.currentTimeMillis();
        int res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        Set<String> list_key_set = Sets.newHashSet();
        Set<String> list_null_key_set = Sets.newHashSet();
        for(long uid : map.keySet()) {
            String key = String.format(user_chat_group_list_key, chatInfo.projectName, uid);
            String null_key = String.format(user_chat_group_list_null_key, chatInfo.projectName, uid);
            list_key_set.add(key);
            list_null_key_set.add(null_key);
        }
        if(res != pools.size()){
            cache.multiDelete(Lists.newArrayList(list_key_set));
            cache.multiDelete(Lists.newArrayList(list_null_key_set));
        } else {
            Map<String, Object> redisMap = Maps.newHashMap();
            for(UserChatGroupPool pool : pools) {
                String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
                redisMap.put(key, pool);
            }
            cache.multiSetObject(redisMap, AbstractChatInfo.redis_overtime_long);

            Map<String, Boolean> exists_map = cache.pipExists(Lists.newArrayList(list_key_set));
            Map<String, Boolean> exists_null_map = cache.pipExists(Lists.newArrayList(list_null_key_set));
            List<RedisKeySortMemObj> list = Lists.newArrayList();
            for(Map.Entry<Long, List<UserChatGroupPool>> entry : map.entrySet()) {
                String key = String.format(user_chat_group_list_key, chatInfo.projectName, entry.getKey());
                String null_key = String.format(user_chat_group_list_null_key, chatInfo.projectName, entry.getKey());
                if(exists_map.get(key) || exists_null_map.get(null_key)) {
                    for(UserChatGroupPool pool : entry.getValue()) {
                        UserChatGroup userChatGroup = new UserChatGroup();
                        userChatGroup.setUid(pool.getUid());
                        userChatGroup.setCgid(pool.getCgid());
                        RedisKeySortMemObj one = new RedisKeySortMemObj(key, userChatGroup, pool.getCtime());
                        list.add(one);
                    }
                }
            }
            if(list.size() > 0) {
                cache.pipZaddObject(list);
            }
            if(list_null_key_set.size() > 0) cache.multiDelete(Lists.newArrayList(list_null_key_set));
        }
        return res;
    }

    @Override
    public int delete(List<UserChatGroupPool> pools) throws DataAccessException {
        Map<Long, List<Object>> map = Maps.newHashMap();
        for (UserChatGroupPool pool : pools) {
            List<Object> list = map.get(pool.getUid());
            if(list == null) {
                list = Lists.newArrayList();
                map.put(pool.getUid(), list);
            }
            UserChatGroup userChatGroup = new UserChatGroup();
            userChatGroup.setUid(pool.getUid());
            userChatGroup.setCgid(pool.getCgid());
            list.add(userChatGroup);
        }
        String sqlPrev = "delete from %s where ";
        String sqlValueFormat = "(uid = %d and cgid = %d)";
        List<String> sqlValueList = Lists.newArrayList();
        for (UserChatGroupPool pool : pools) {
            String sqlValue = String.format(sqlValueFormat, pool.getUid(), pool.getCgid());
            sqlValueList.add(sqlValue);
        }
        String sqlValues = StringUtils.join(sqlValueList.toArray(), " or ");
        String sql = sqlPrev + sqlValues;
        sql = String.format(sql, getTable());
        long startTime = System.currentTimeMillis();
        int res = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        List<String> redisList = Lists.newArrayList();
        for(UserChatGroupPool pool : pools) {
            String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
            redisList.add(key);
        }
        cache.multiDelete(redisList);

        Set<String> list_key_set = Sets.newHashSet();
        for(long uid : map.keySet()) {
            String key = String.format(user_chat_group_list_key, chatInfo.projectName, uid);
            list_key_set.add(key);
        }
        Map<String, Boolean> exists_map = cache.pipExists(Lists.newArrayList(list_key_set));
        for(Map.Entry<Long, List<Object>> entry : map.entrySet()) {
            String key = String.format(user_chat_group_list_key, chatInfo.projectName, entry.getKey());
            if(exists_map.get(key)) {
                cache.zremMultiObject(key, entry.getValue());
            }
        }
        return res;
    }

    @Override
    public int update(UserChatGroupPool pool) throws DataAccessException {
        String sql = "update %s set `type` = %d, `status` = %d, inviteUid = %d, utime = %d where uid = %d and cgid = %d";
        sql = String.format(sql, getTable(), pool.getType(), pool.getStatus(), pool.getInviteUid(), System.currentTimeMillis(), pool.getUid(), pool.getCgid());
        long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
        if(ret > 0) {
            String key = String.format(user_chat_group_pool_object_key, chatInfo.projectName, pool.getUid(), pool.getCgid());
            cache.delete(key);
            this.load(Lists.newArrayList(pool));
        }
        return ret;
    }

    @Override
    public List<UserChatGroupPool> list(long uid) throws Exception {
        List<UserChatGroupPool> res = Lists.newArrayList();
        String key = String.format(user_chat_group_list_key, chatInfo.projectName, uid);
        String null_key = String.format(user_chat_group_list_null_key, chatInfo.projectName, uid);
        if(!cache.exists(key) && !cache.exists(null_key)) {
            String sql = "select * from %s where uid = %d order by `ctime` desc";
            sql = String.format(sql, getTable(), uid);
            long startTime = System.currentTimeMillis();
            res = this.jdbcTemplate.query(sql, new UserChatGroupPoolRowMapper());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
            if(res.size() > 0){
                Map<Object, Double> map = Maps.newHashMap();
                for(UserChatGroupPool one : res){
                    Double score = (double) one.getCtime();
                    UserChatGroup userChatGroup = new UserChatGroup();
                    userChatGroup.setUid(one.getUid());
                    userChatGroup.setCgid(one.getCgid());
                    map.put(userChatGroup, score);
                }
                if(map.size() > 0){
                    cache.zaddMultiObject(key, map);
                    cache.expire(key, AbstractChatInfo.redis_overtime_long);
                }
            } else{
                cache.setString(null_key, AbstractChatInfo.redis_overtime_long, "true");
            }
        } else {
            Set<Object> set = cache.zrevrangeByScoreObject(key, Double.MAX_VALUE, 0, null);
            if(null != set && set.size() > 0){
                List<UserChatGroupPool> params = Lists.newArrayList();
                for(Object one : set){
                    UserChatGroup userChatGroup = (UserChatGroup) one;
                    UserChatGroupPool param = new UserChatGroupPool();
                    param.setUid(userChatGroup.getUid());
                    param.setCgid(userChatGroup.getCgid());
                    params.add(param);
                }
                res = this.load(params);
            }
        }
        return res;
    }

    class UserChatGroupPoolResultSetExtractor implements ResultSetExtractor<UserChatGroupPool> {
        @Override
        public UserChatGroupPool extractData(ResultSet rs) throws SQLException, DataAccessException {
            UserChatGroupPool res = new UserChatGroupPool();
            res.setUid(rs.getLong("uid"));
            res.setCgid(rs.getLong("cgid"));
            res.setType(rs.getInt("type"));
            res.setStatus(rs.getInt("status"));
            res.setInviteUid(rs.getLong("inviteUid"));
            res.setCtime(rs.getLong("ctime"));
            res.setUtime(rs.getLong("utime"));
            return res;
        }
    }

    class UserChatGroupPoolRowMapper implements RowMapper<UserChatGroupPool> {
        @Override
        public UserChatGroupPool mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            UserChatGroupPool res = new UserChatGroupPool();
            res.setUid(rs.getLong("uid"));
            res.setCgid(rs.getLong("cgid"));
            res.setType(rs.getInt("type"));
            res.setStatus(rs.getInt("status"));
            res.setInviteUid(rs.getLong("inviteUid"));
            res.setCtime(rs.getLong("ctime"));
            res.setUtime(rs.getLong("utime"));
            return res;
        }
    }

}
