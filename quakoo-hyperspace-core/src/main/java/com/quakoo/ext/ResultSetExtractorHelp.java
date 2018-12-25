package com.quakoo.ext;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.space.annotation.domain.HyperspaceColumn;
import com.quakoo.space.model.FieldInfo;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.IllegalClassException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ResultSetExtractorHelp<T> implements ResultSetExtractor<T> {

    private Class<T> entityClass;

    private List<FieldInfo> fields = Lists.newArrayList(); // 所有的列信息

    public ResultSetExtractorHelp(Class<T> clazz) {
        try {
            entityClass = clazz;
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(entityClass)
                    .getPropertyDescriptors();
            for (PropertyDescriptor one : propertyDescriptors) {
                String fieldName = one.getName();
                if (!"class".equals(fieldName)) {
                    Field field = ReflectUtil
                            .getFieldByName(fieldName, entityClass);
                    if (field != null) {
                        HyperspaceColumn autowareMap = field
                                .getAnnotation(HyperspaceColumn.class);
                        if (autowareMap != null && !autowareMap.isDbColumn()) {
                            continue;
                        }
                        String name = one.getName();
                        String dbName = name;
                        Method writeMethod = one.getWriteMethod();
                        Method readMethod = one.getReadMethod();
                        if (autowareMap != null) {
                            dbName = autowareMap.column();
                        }
                        FieldInfo fieldInfo = new FieldInfo(field, name, dbName,
                                writeMethod, readMethod);
                        fields.add(fieldInfo);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalClassException("ResultSetExtractorHelp is error");
        }
    }

    @Override
    public T extractData(ResultSet rs) throws SQLException, DataAccessException {
        if(rs.next()) {
            try {
                Object o = entityClass.newInstance();
                for (FieldInfo info : fields) {
                    String c = info.getDbName();
                    if (c.startsWith("`") && c.endsWith("`")) {
                        c = c.substring(1, c.length() - 1);
                    }
                    Method writeMethod = info.getWriteMethod();
                    Type type = info.getField().getGenericType();
                    writeMethod.invoke(o,
                            ReflectUtil.getValueFormRsByType(type, rs, c));
                }
                return (T) o;
            } catch (Exception e) {
                throw new SQLException(e);
            }
        } else return null;
    }
}
