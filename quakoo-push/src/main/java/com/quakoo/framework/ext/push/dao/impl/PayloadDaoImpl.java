//package com.quakoo.framework.ext.push.dao.impl;
//
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import javax.annotation.Resource;
//
//import com.quakoo.framework.ext.push.dao.BaseDao;
//import com.quakoo.framework.ext.push.dao.PayloadDao;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.dao.DataAccessException;
//import org.springframework.jdbc.core.BatchPreparedStatementSetter;
//import org.springframework.jdbc.core.ResultSetExtractor;
//import org.springframework.jdbc.core.RowMapper;
//import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.quakoo.baseFramework.jackson.JsonUtils;
//import com.quakoo.framework.ext.push.model.Payload;
//
//public class PayloadDaoImpl extends BaseDao implements PayloadDao, InitializingBean {
//
//    private Logger logger = LoggerFactory.getLogger(PayloadDaoImpl.class);
//
//	private String object_key;
//
//	@Resource
//    private DataFieldMaxValueIncrementer payloadMaxValueIncrementer;
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        object_key = pushInfo.projectName + "_payload_object_%d";
//    }
//
//    private String getTable(long id){
//		int index = (int) id % pushInfo.payload_table_names.size();
//		return pushInfo.payload_table_names.get(index);
//	}
//
//    @Override
//    public List<Long> getPayloadIds(int size) throws DataAccessException {
//        List<Long> res = Lists.newArrayList();
//        for(int i = 0; i < size; i++) {
//            long id = payloadMaxValueIncrementer.nextLongValue();
//            res.add(id);
//        }
//        return res;
//    }
//
//    private boolean sql_inj(String str){
//        int srcLen, decLen = 0;
//        str = str.toLowerCase().trim();
//        srcLen = str.length();
//        str = str.replace("exec", "");
//        str = str.replace("delete", "");
//        str = str.replace("master", "");
//        str = str.replace("truncate", "");
//        str = str.replace("declare", "");
//        str = str.replace("create", "");
//        str = str.replace("xp_", "no");
//        decLen = str.length();
//        if (srcLen == decLen) return false;
//        else return true;
//    }
//
//    @Override
//    public List<Payload> insert(List<Payload> payloads) throws DataAccessException {
//        for(Iterator<Payload> it = payloads.iterator(); it.hasNext();) {
//            Payload payload = it.next();
//            String extra = JsonUtils.toJson(payload.getExtra());
//            if(sql_inj(payload.getTitle())) {
//                it.remove();
//                continue;
//            }
//            if(sql_inj(payload.getContent())) {
//                it.remove();
//                continue;
//            }
//            if(sql_inj(extra)) {
//                it.remove();
//            }
//        }
//        String sqlPrev = "insert ignore into %s (id, title, content, extra, `time`, platform) values ";
//        String sqlFormat = "(%d, '%s', '%s', '%s', %d, %d)";
//        Map<String, List<Payload>> maps = Maps.newHashMap();
//        for(Payload payload : payloads){
//            payload.setTime(System.currentTimeMillis());
//            String tableName = getTable(payload.getId());
//            List<Payload> list = maps.get(tableName);
//            if(null == list){
//                list = Lists.newArrayList();
//                maps.put(tableName, list);
//            }
//            list.add(payload);
//        }
//        List<String> sqls = Lists.newArrayList();
//        for(Map.Entry<String, List<Payload>> entry : maps.entrySet()) {
//            String tableName = entry.getKey();
//            List<Payload> list = entry.getValue();
//            List<String> sqlValueList = Lists.newArrayList();
//            for(Payload payload : list){
//                String extra = JsonUtils.toJson(payload.getExtra());
//                String sqlValue = String.format(sqlFormat, payload.getId(), payload.getTitle(),
//                        payload.getContent(), extra, payload.getTime(), payload.getPlatform());
//                sqlValueList.add(sqlValue);
//            }
//            String sqlValues = StringUtils.join(sqlValueList, ",");
//            String sql = String.format(sqlPrev, tableName) + sqlValues;
//            sqls.add(sql);
//        }
//        long startTime = System.currentTimeMillis();
//        int[] resList = this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sqls : " + sqls.toString());
//        Map<String, Object> redisMap = Maps.newHashMap();
//        for(Payload payload : payloads) {
//            String key = String.format(object_key, payload.getId());
//            redisMap.put(key, payload);
//        }
//        if(redisMap.size() > 0) cache.multiSetObject(redisMap, pushInfo.redis_overtime_long);
//        return payloads;
//    }
//
//    @Override
//	public Payload insert(Payload payload) throws DataAccessException {
//        boolean res = false;
//        long id = payload.getId();
//        String title = payload.getTitle();
//        String content = payload.getContent();
//        String extra = JsonUtils.toJson(payload.getExtra());
//        long time = System.currentTimeMillis();
//        int platform = payload.getPlatform();
//        payload.setTime(time);
//        String tableName = getTable(id);
//        String sql = "insert ignore into %s (id, title, content, extra, `time`, platform) values (?, ?, ?, ?, ?, ?)";
//        sql = String.format(sql, tableName);
//        long startTime = System.currentTimeMillis();
//        int ret = this.jdbcTemplate.update(sql, id, title, content, extra, time, platform);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//        res = ret > 0 ? true : false;
//
//        if(res){
//            payload.setId(id);
//            String key = String.format(object_key, id);
//            cache.setObject(key, pushInfo.redis_overtime_long, payload);
//            return payload;
//        } else {
//            return null;
//        }
//	}
//
//	@Override
//	public Payload load(long id) throws DataAccessException {
//        String key = String.format(object_key, id);
//        Object obj = cache.getObject(key, null);
//        if(null != obj){
//            return (Payload) obj;
//        } else {
//            String tableName = getTable(id);
//            String sql = "select * from %s where id = %d";
//            sql = String.format(sql, tableName, id);
//            long startTime = System.currentTimeMillis();
//            Payload payload = this.jdbcTemplate
//                    .query(sql, new PayloadResultSetExtractor());
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//            if(null != payload){
//                cache.setObject(key, pushInfo.redis_overtime_long, payload);
//            }
//            return payload;
//        }
//	}
//
//	@Override
//	public List<Payload> load(List<Long> ids) throws DataAccessException {
//        List<String> object_key_list = Lists.newArrayList();
//        Map<Long, String> id_key_map = Maps.newHashMap();
//        for(long id : ids){
//            String key = String.format(object_key, id);
//            object_key_list.add(key);
//            id_key_map.put(id, key);
//        }
//        Map<Long, Payload> res_map = Maps.newHashMap();
//        Map<String, Object> redis_map = cache.multiGetObject(object_key_list, null);
//        List<Long> non_ids = Lists.newArrayList();
//        for(long id : ids){
//            Object obj = redis_map.get(id_key_map.get(id));
//            if(null == obj){
//                non_ids.add(id);
//            } else {
//                res_map.put(id, (Payload) obj);
//            }
//        }
//        if(non_ids.size() > 0){
//            String sql = "select * from %s where id in (%s)";
//            Map<String, List<Long>> maps = Maps.newHashMap();
//            for(long id : non_ids){
//                String tableName = getTable(id);
//                List<Long> list = maps.get(tableName);
//                if(null == list){
//                    list = Lists.newArrayList();
//                    maps.put(tableName, list);
//                }
//                list.add(id);
//            }
//            List<String> sqls = Lists.newArrayList();
//            for(Entry<String, List<Long>> entry : maps.entrySet()){
//                String tableName = entry.getKey();
//                List<Long> list = entry.getValue();
//                String one_sql = String.format(sql, tableName, StringUtils.join(list, ","));
//                sqls.add(one_sql);
//            }
//            Map<String, Object> redis_insert_map = Maps.newHashMap();
//            for(String one_sql : sqls){
//                long startTime = System.currentTimeMillis();
//                List<Payload> list = this.jdbcTemplate.query(one_sql,
//                        new PayloadRowMapper());
//                logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//                for(Payload payload : list){
//                    long id = payload.getId();
//                    res_map.put(id, payload);
//                    redis_insert_map.put(String.format(object_key, id), payload);
//                }
//            }
//            cache.multiSetObject(redis_insert_map, pushInfo.redis_overtime_long);
//        }
//        List<Payload> res = Lists.newArrayList();
//        for(long id : ids){
//            Payload payload = res_map.get(id);
//            res.add(payload);
//        }
//        return res;
//	}
//
//	class PayloadRowMapper implements RowMapper<Payload> {
//		@Override
//		public Payload mapRow(ResultSet rs, int rowNum) throws SQLException {
//			Payload payload = new Payload();
//			payload.setContent(rs.getString("content"));
//			Map<String, String> extra = JsonUtils.fromJson(rs.getString("extra"),
//					new TypeReference<Map<String, String>>() {});
//			payload.setExtra(extra);
//		    payload.setId(rs.getLong("id"));
//		    payload.setTitle(rs.getString("title"));
//		    payload.setTime(rs.getLong("time"));
//		    payload.setPlatform(rs.getInt("platform"));
//			return payload;
//		}
//	}
//
//	class PayloadResultSetExtractor implements ResultSetExtractor<Payload> {
//		@Override
//		public Payload extractData(ResultSet rs) throws SQLException,
//				DataAccessException {
//			if(rs.next()){
//				Payload payload = new Payload();
//				payload.setContent(rs.getString("content"));
//				Map<String, String> extra = JsonUtils.fromJson(rs.getString("extra"),
//						new TypeReference<Map<String, String>>() {});
//				payload.setExtra(extra);
//			    payload.setId(rs.getLong("id"));
//			    payload.setTitle(rs.getString("title"));
//			    payload.setTime(rs.getLong("time"));
//                payload.setPlatform(rs.getInt("platform"));
//				return payload;
//			} else
//				return null;
//		}
//	}
//}
