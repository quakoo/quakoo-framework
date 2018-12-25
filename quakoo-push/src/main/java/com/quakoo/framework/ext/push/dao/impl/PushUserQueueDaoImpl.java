package com.quakoo.framework.ext.push.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueDao;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;

import com.quakoo.framework.ext.push.model.PushUserQueue;

public class PushUserQueueDaoImpl extends BaseDao implements PushUserQueueDao {

	private String getTable(long uid){
		int index = (int) uid % pushInfo.push_user_queue_table_names.size();
		return pushInfo.push_user_queue_table_names.get(index);
	}
	
	@Override
	public void insert(PushUserQueue one) throws DataAccessException {
		long uid = one.getUid();
		String tableName = this.getTable(uid);
		String sql = "insert ignore into %s (uid) values (?)";
		sql = String.format(sql, tableName);
		this.jdbcTemplate.update(sql, uid);
	}

	@Override
	public List<PushUserQueue> getList(String table_name, long index, int size)
			throws DataAccessException {
		String sql = "select * from %s where `index` > %d order by `index` asc limit %d";
		sql = String.format(sql, table_name, index, size);
		List<PushUserQueue> list = this.jdbcTemplate.query(sql, 
				new PushUserQueueRowMapper());
		return list;
	}
	
	class PushUserQueueRowMapper implements RowMapper<PushUserQueue> {
		@Override
		public PushUserQueue mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			PushUserQueue res = new PushUserQueue();
			res.setIndex(rs.getLong("index"));
			res.setUid(rs.getLong("uid"));
			return res;
		}
	}

}
