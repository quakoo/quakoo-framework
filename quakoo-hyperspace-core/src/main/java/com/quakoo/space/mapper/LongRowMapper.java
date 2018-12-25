package com.quakoo.space.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;


public class LongRowMapper implements RowMapper {
    private String column = "id";

    public LongRowMapper(String column) {
        this.column = column;
    }

    @Override
    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getLong(column);
    }
}
