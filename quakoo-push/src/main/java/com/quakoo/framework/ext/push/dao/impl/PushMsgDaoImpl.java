package com.quakoo.framework.ext.push.dao.impl;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.push.dao.BaseDao;
import com.quakoo.framework.ext.push.dao.PushMsgDao;
import com.quakoo.framework.ext.push.model.PushMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class PushMsgDaoImpl extends BaseDao implements PushMsgDao {

    private Logger logger = LoggerFactory.getLogger(PushMsgDaoImpl.class);

    @Resource
    private DataFieldMaxValueIncrementer pushMsgMaxValueIncrementer;

    @Override
    public long createId() {
        long id = pushMsgMaxValueIncrementer.nextLongValue();
        return id;
    }

    private boolean sql_inj(String str){
        int srcLen, decLen = 0;
        str = str.toLowerCase().trim();
        srcLen = str.length();
        str = str.replace("exec", "");
        str = str.replace("delete", "");
        str = str.replace("master", "");
        str = str.replace("truncate", "");
        str = str.replace("declare", "");
        str = str.replace("create", "");
        str = str.replace("xp_", "no");
        decLen = str.length();
        if (srcLen == decLen) return false;
        else return true;
    }

    @Override
    public void insert(final List<PushMsg> list) throws DataAccessException {
        for(Iterator<PushMsg> it = list.iterator(); it.hasNext();) {
            PushMsg pushMsg = it.next();
            String extra = JsonUtils.toJson(pushMsg.getExtra());
            if(sql_inj(pushMsg.getTitle())) {
                pushMsg.setTitle("<sql注入>");
            }
            if(sql_inj(pushMsg.getContent())) {
                pushMsg.setContent("<sql注入>");
            }
            if(sql_inj(extra)) {
                pushMsg.setExtra(null);
            }
        }
        String sql = "insert ignore into push_msg (id, title, content, extra, type, uid, uids, platform, `time`, status) values (?,?,?,?,?,?,?,?,?,?)";
        BatchPreparedStatementSetter batchPreparedStatementSetter = new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PushMsg one = list.get(i);
                ps.setLong(1, one.getId());
                ps.setString(2, one.getTitle());
                ps.setString(3, one.getContent());
                String extra = JsonUtils.toJson(one.getExtra());
                ps.setString(4, extra);
                ps.setInt(5, one.getType());
                ps.setLong(6, one.getUid());
                ps.setString(7, one.getUids());
                ps.setInt(8, one.getPlatform());
                ps.setLong(9, one.getTime());
                ps.setInt(10, one.getStatus());
            }
            @Override
            public int getBatchSize() {
                return list.size();
            }
        };
        long startTime = System.currentTimeMillis();
        int[] res = jdbcTemplate.batchUpdate(sql, batchPreparedStatementSetter);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql + " , pushMsgs : " + list.toString() + " , res : " + Lists.newArrayList(res).toString());
    }

    @Override
    public void update(final List<PushMsg> list) throws DataAccessException {
        String sql = "replace into push_msg (id, title, content, extra, type, uid, uids, platform, `time`, status) values (?,?,?,?,?,?,?,?,?,?)";
        BatchPreparedStatementSetter batchPreparedStatementSetter = new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PushMsg one = list.get(i);
                ps.setLong(1, one.getId());
                ps.setString(2, one.getTitle());
                ps.setString(3, one.getContent());
                String extra = JsonUtils.toJson(one.getExtra());
                ps.setString(4, extra);
                ps.setInt(5, one.getType());
                ps.setLong(6, one.getUid());
                ps.setString(7, one.getUids());
                ps.setInt(8, one.getPlatform());
                ps.setLong(9, one.getTime());
                ps.setInt(10, one.getStatus());
            }
            @Override
            public int getBatchSize() {
                return list.size();
            }
        };
        long startTime = System.currentTimeMillis();
        int[] res = jdbcTemplate.batchUpdate(sql, batchPreparedStatementSetter);
        logger.info("===== sql time : " + (System.currentTimeMillis() - startTime) + " , sql : " + sql + " , pushMsgs : " + list.toString() + " , res : " + Lists.newArrayList(res).toString());
    }

}

