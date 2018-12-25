package com.quakoo.space.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class IntegerRowMapper implements RowMapper {
    private final String column;

    public IntegerRowMapper(String column) {
        this.column = column;
    }

    @Override
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

        return rs.getInt(column);
    }

}
