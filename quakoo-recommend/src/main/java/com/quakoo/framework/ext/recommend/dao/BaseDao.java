package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;

@Data
public class BaseDao {

    @Resource
    protected JdbcTemplate jdbcTemplate;

    @Resource
    protected AbstractRecommendInfo recommendInfo;

}
