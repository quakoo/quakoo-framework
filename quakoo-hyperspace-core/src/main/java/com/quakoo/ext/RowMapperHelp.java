package com.quakoo.ext;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.space.annotation.domain.HyperspaceColumn;
import com.quakoo.space.model.FieldInfo;
import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RowMapperHelp<T> implements RowMapper<T> {

    private Class<T> entityClass;

    private List<FieldInfo> fields = Lists.newArrayList(); // 所有的列信息

    public RowMapperHelp(Class<T> clazz){
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
                        boolean isJson=false;
                        if (autowareMap != null) {
                            if(StringUtils.isNotBlank(autowareMap.column())){
                                dbName = autowareMap.column();
                            }
                            isJson=autowareMap.isJson();
                        }
                        FieldInfo fieldInfo = new FieldInfo(field, name, dbName,
                                writeMethod, readMethod,isJson);
                        fields.add(fieldInfo);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalClassException("RowMapperHelp is error");
        }
    }

    @Override
    public T mapRow(ResultSet rs, int num) throws SQLException {
        FieldInfo fieldInfo = null;
        try {
            Object o = entityClass.newInstance();
            for (FieldInfo info : fields) {
                fieldInfo = info;
                String c = info.getDbName();
                if (c.startsWith("`") && c.endsWith("`")) {
                    c = c.substring(1, c.length() - 1);
                }
                Method writeMethod = info.getWriteMethod();
                Type type = info.getField().getGenericType();
                boolean isJson = info.isJson();
                if(isJson) {
                    String jsonValue = rs.getString(c);
                    writeMethod.invoke(o, JsonUtils.parse(jsonValue, type));
                } else writeMethod.invoke(o, ReflectUtil.getValueFormRsByType(type, rs, c));
            }
            return (T) o;
        } catch (Exception e) {
            String msg = "";
            if(fieldInfo != null) msg = fieldInfo.toString();
            throw new SQLException(msg, e);
        }
    }

}
