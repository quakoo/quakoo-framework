package com.quakoo.framework.ext.push.job;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.push.AbstractPushInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.List;

public class CleanJob {

    private Logger logger = LoggerFactory.getLogger(CleanJob.class);

    private int step_day = 1000 * 60 * 60 * 24 * 1;

    @Resource
    private AbstractPushInfo pushInfo;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private void cleanPayload(long time) {
        try {
            List<String> tableNames = pushInfo.payload_table_names;
            String sqlFormat = "delete from %s where time < %s";
            List<String> sqls = Lists.newArrayList();
            for(String tableName : tableNames) {
                sqls.add(String.format(sqlFormat, tableName, time));
            }
            this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void handle() {
        long currentTime = System.currentTimeMillis();
        cleanPayload(currentTime - step_day);
    }

}
