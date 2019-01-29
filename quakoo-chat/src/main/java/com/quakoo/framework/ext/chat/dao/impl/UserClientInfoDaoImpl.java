//package com.quakoo.framework.ext.chat.dao.impl;
//
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.dao.DataAccessException;
//import org.springframework.jdbc.core.ResultSetExtractor;
//
//import com.quakoo.framework.ext.chat.AbstractChatInfo;
//import com.quakoo.framework.ext.chat.dao.BaseDaoHandle;
//import com.quakoo.framework.ext.chat.dao.UserClientInfoDao;
//import com.quakoo.framework.ext.chat.model.UserClientInfo;
//
//public class UserClientInfoDaoImpl extends BaseDaoHandle implements UserClientInfoDao {
//
//    private Logger logger = LoggerFactory.getLogger(UserClientInfoDaoImpl.class);
//
//    private static final String object_key = "%s_user_client_info_uid_%d_clientId_%s";
//
//    private String getTable(long uid){
//		int index = (int) uid % chatInfo.user_client_info_table_names.size();
//		return chatInfo.user_client_info_table_names.get(index);
//	}
//
//	public boolean insert(UserClientInfo one) throws DataAccessException {
//		boolean res = false;
//		long uid = one.getUid();
//		String clientId = one.getClientId();
//		long thirdId = one.getThirdId();
//		long mid = one.getMid();
//		int type = one.getType();
//		long ctime = one.getCtime();
//		String tableName = getTable(uid);
//		String sql = "insert into %s (uid, clientId, thirdId, mid, type, ctime) values (?, ?, ?, ?, ?, ?)";
//		sql = String.format(sql, tableName);
//		long startTime = System.currentTimeMillis();
//		int ret = this.jdbcTemplate.update(sql, uid, clientId, thirdId, mid, type, ctime);
//        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//        res = ret > 0 ? true : false;
//
//		if(res){
//			String key = String.format(object_key, chatInfo.projectName, uid, clientId);
//			cache.setObject(key, AbstractChatInfo.redis_overtime_long, one);
//		}
//		return res;
//	}
//
//	public UserClientInfo load(UserClientInfo one) throws DataAccessException {
//		long uid = one.getUid();
//		String clientId = one.getClientId();
//		String key = String.format(object_key, chatInfo.projectName, uid, clientId);
//		Object obj = cache.getObject(key, null);
//		if(null != obj){
//			return (UserClientInfo) obj;
//		} else {
//			String tableName = getTable(uid);
//			String sql = "select * from %s where uid = %d and clientId = '%s'";
//			sql = String.format(sql, tableName, uid, clientId);
//			long startTime = System.currentTimeMillis();
//			UserClientInfo clientInfo = this.jdbcTemplate.query(sql,
//					new UserClientInfoResultSetExtractor());
//            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
//
//            if(null != clientInfo){
//				cache.setObject(key, AbstractChatInfo.redis_overtime_long, clientInfo);
//			}
//			return clientInfo;
//		}
//	}
//
//	class UserClientInfoResultSetExtractor implements
//			ResultSetExtractor<UserClientInfo> {
//		@Override
//		public UserClientInfo extractData(ResultSet rs) throws SQLException,
//				DataAccessException {
//			if (rs.next()) {
//				UserClientInfo res = new UserClientInfo();
//				res.setClientId(rs.getString("clientId"));
//				res.setCtime(rs.getLong("ctime"));
//				res.setMid(rs.getLong("mid"));
//				res.setThirdId(rs.getLong("thirdId"));
//				res.setType(rs.getInt("type"));
//				res.setUid(rs.getLong("uid"));
//				return res;
//			} else
//				return null;
//		}
//	}
//
//}
