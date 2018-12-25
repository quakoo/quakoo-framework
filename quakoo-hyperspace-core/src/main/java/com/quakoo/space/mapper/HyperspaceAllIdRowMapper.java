package com.quakoo.space.mapper;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.quakoo.space.annotation.HyperspaceAllId;
import com.quakoo.space.annotation.HyperspaceCombinationId;
import com.quakoo.space.annotation.HyperspacePrimaryId;
import com.quakoo.space.model.FieldInfo;
import org.springframework.jdbc.core.RowMapper;

import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.space.JdbcBaseDao;

/**
 *
 */
public class HyperspaceAllIdRowMapper implements RowMapper<HyperspaceAllId> {
    private JdbcBaseDao jdbcBaseDao;

    public HyperspaceAllIdRowMapper(JdbcBaseDao jdbcBaseDao) {
        this.jdbcBaseDao = jdbcBaseDao;
    }

    @Override
    public HyperspaceAllId mapRow(ResultSet rs, int rowNum) throws SQLException {

        FieldInfo primaryFieldInfo = jdbcBaseDao.getPrimaryFieldInfo();
        List<FieldInfo> combinationFieldInfos = jdbcBaseDao.getCombinationFieldInfos();

        HyperspaceAllId hyperspaceAllId = new HyperspaceAllId();
        if (primaryFieldInfo != null) {
            long id = rs.getLong(primaryFieldInfo.getDbName());
            HyperspacePrimaryId hyperspacePrimaryId = new HyperspacePrimaryId(id);
            FieldInfo shardinFieldInfo = jdbcBaseDao.getShardingFieldInfo();
            if (shardinFieldInfo != primaryFieldInfo) {
                String c = shardinFieldInfo.getDbName();
                if (c.startsWith("`") && c.endsWith("`")) {
                    c = c.substring(1, c.length() - 1);
                }
                hyperspacePrimaryId.setSharding(rs.getLong(c));
            }
            hyperspaceAllId.setHyperspacePrimaryId(hyperspacePrimaryId);
        }
        if (combinationFieldInfos.size() > 0) {
            HyperspaceCombinationId hyperspaceCombinationId = new HyperspaceCombinationId();

            for (FieldInfo info : combinationFieldInfos) {
                String c = info.getDbName();
                if (c.startsWith("`") && c.endsWith("`")) {
                    c = c.substring(1, c.length() - 1);
                }

                Type type = info.getField().getGenericType();
                hyperspaceCombinationId.getList().add(ReflectUtil.getValueFormRsByType(type, rs, c));
            }
            hyperspaceAllId.setHyperspaceCombinationId(hyperspaceCombinationId);
        }

        return hyperspaceAllId;

    }
}
