package com.quakoo.space.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;


public class StringRowMapper implements RowMapper {
    private String column = "id";

    public StringRowMapper(String column) {
        this.column = column;
    }

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(column);
    }
}
