package com.quakoo.space.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quakoo.space.enums.JsonTypeReference;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FieldInfo {

    private Field field;

    private boolean isJson;

    private String name;

    private String dbName;

    private Method writeMethod;

    private Method readMethod;

    public FieldInfo(Field field, String name, String dbName, Method writeMethod, Method readMethod,boolean isJson) {
        super();
        this.field = field;
        this.name = name;
        this.dbName = dbName;
        this.writeMethod = writeMethod;
        this.readMethod = readMethod;
        this.isJson=isJson;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }


    public boolean isJson() {
        return isJson;
    }

    public void setJson(boolean json) {
        isJson = json;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof FieldInfo) {
            FieldInfo other = (FieldInfo) obj;
            if (this.getName().equals(other.getName())) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String toString() {
        return "FieldInfo [field=" + field + ", name=" + name + ", dbName=" + dbName + ", writeMethod=" + writeMethod
                + ", readMethod=" + readMethod + "]";
    }

}
