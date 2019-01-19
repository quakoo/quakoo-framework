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

    private void cleanPushMsg(long time) {
        try {
            String sql = "delete from push_msg where time < %s";
            this.jdbcTemplate.update(String.format(sql, time));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void handle() {
        long currentTime = System.currentTimeMillis();
        cleanPushMsg(currentTime - step_day);
    }

}
