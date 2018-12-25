package com.quakoo.framework.ext.push.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueInfoDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.quakoo.framework.ext.push.model.PushUserQueueInfo;

public class PushUserQueueInfoDaoImpl extends BaseDao implements PushUserQueueInfoDao, InitializingBean {
	
	private String object_key;

    @Override
    public void afterPropertiesSet() throws Exception {
        object_key = pushInfo.projectName + "_push_user_queue_%s";
    }

    @Override
	public void insert(PushUserQueueInfo one) throws DataAccessException {
		String tableName = one.getTableName();
		String sql = "insert ignore into push_user_queue_info (`tableName`) values (?)";
		int ret = this.jdbcTemplate.update(sql, tableName);
		if(ret > 0){
			String key = String.format(object_key, tableName);
			cache.setObject(key, pushInfo.redis_overtime_long, one);
		}
	}

	@Override
	public PushUserQueueInfo load(String tableName) throws DataAccessException {
		String key = String.format(object_key, tableName);
		Object obj = cache.getObject(key, null);
		if(null != obj){
			return (PushUserQueueInfo) obj;
		} else {
			String sql = "select * from push_user_queue_info where tableName = '%s'";
			sql = String.format(sql, tableName);
			PushUserQueueInfo pushUserQueueInfo = this.jdbcTemplate
					.query(sql, new PushUserQueueInfoResultSetExtractor());
			if(null != pushUserQueueInfo){
				cache.setObject(key, pushInfo.redis_overtime_long, pushUserQueueInfo);
			}
			return pushUserQueueInfo;
		}
	}
	
	@Override
	public boolean update(PushUserQueueInfo one) throws DataAccessException {
		boolean res = false;
		String tableName = one.getTableName();
		long phaqid = one.getPhaqid();
		int end = one.getEnd();
		long index = one.getIndex();
		String sql = "update push_user_queue_info set phaqid = ?, `index` = ?, end = ? where `tableName` = ?";
		int ret = this.jdbcTemplate.update(sql, phaqid, index, end, tableName);
		res = ret > 0 ? true : false;
		if(res) {
			String key = String.format(object_key, tableName);
			cache.setObject(key, pushInfo.redis_overtime_long, one);
		}
		return res;
	}
	
	class PushUserQueueInfoResultSetExtractor implements ResultSetExtractor<PushUserQueueInfo> {
		@Override
		public PushUserQueueInfo extractData(ResultSet rs) throws SQLException,
				DataAccessException {
			if(rs.next()){
				PushUserQueueInfo res = new PushUserQueueInfo();
				res.setEnd(rs.getInt("end"));
				res.setIndex(rs.getLong("index"));
				res.setPhaqid(rs.getLong("phaqid"));
				res.setTableName(rs.getString("tableName"));
				return res;
			} else
				return null;
		}
	}

}
