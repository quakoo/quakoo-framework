package com.quakoo.framework.ext.push.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushUserQueueInfoDao;
import com.quakoo.framework.ext.push.model.PushUserQueueInfo;


/**
 * 推送用户队列信息表(用于记录推送到什么位置)
 * class_name: PushUserQueueInfoDaoImpl
 * package: com.quakoo.framework.ext.push.dao.impl
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:52
 **/
public class PushUserQueueInfoDaoImpl extends BaseDao implements PushUserQueueInfoDao, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(PushUserQueueInfoDaoImpl.class);

	private String object_key;

    @Override
    public void afterPropertiesSet() throws Exception {
        object_key = pushInfo.projectName + "_push_user_queue_%s";
    }

    /**
     * 插入推送用户队列信息表
     * method_name: insert
     * params: [one]
     * return: void
     * creat_user: lihao
     * creat_date: 2019/1/30
     * creat_time: 11:54
     **/
    @Override
	public void insert(PushUserQueueInfo one) throws DataAccessException {
		String tableName = one.getTableName();
		String sql = "insert ignore into push_user_queue_info (`tableName`) values (?)";
        long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, tableName);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
		if(ret > 0){
			String key = String.format(object_key, tableName);
			cache.setObject(key, pushInfo.redis_overtime_long, one);
		}
	}

	/**
     * 获取推送用户队列信息表
	 * method_name: load
	 * params: [tableName]
	 * return: com.quakoo.framework.ext.push.model.PushUserQueueInfo
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:54
	 **/
	@Override
	public PushUserQueueInfo load(String tableName) throws DataAccessException {
		String key = String.format(object_key, tableName);
		Object obj = cache.getObject(key, null);
		if(null != obj){
			return (PushUserQueueInfo) obj;
		} else {
			String sql = "select * from push_user_queue_info where tableName = '%s'";
			sql = String.format(sql, tableName);
            long startTime = System.currentTimeMillis();
			PushUserQueueInfo pushUserQueueInfo = this.jdbcTemplate
					.query(sql, new PushUserQueueInfoResultSetExtractor());
            logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
			if(null != pushUserQueueInfo){
				cache.setObject(key, pushInfo.redis_overtime_long, pushUserQueueInfo);
			}
			return pushUserQueueInfo;
		}
	}

	/**
     * 修改推送用户队列信息表
	 * method_name: update
	 * params: [one]
	 * return: boolean
	 * creat_user: lihao
	 * creat_date: 2019/1/30
	 * creat_time: 11:55
	 **/
	@Override
	public boolean update(PushUserQueueInfo one) throws DataAccessException {
		boolean res = false;
		String tableName = one.getTableName();
		long phaqid = one.getPhaqid();
		int end = one.getEnd();
		long index = one.getIndex();
		String sql = "update push_user_queue_info set phaqid = ?, `index` = ?, end = ? where `tableName` = ?";
        long startTime = System.currentTimeMillis();
		int ret = this.jdbcTemplate.update(sql, phaqid, index, end, tableName);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql);
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
