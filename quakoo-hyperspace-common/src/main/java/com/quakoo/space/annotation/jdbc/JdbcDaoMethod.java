package com.quakoo.space.annotation.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.jdbc.JdbcMethodEnum;

/**
 * dao的方法中 只使用数据库不使用缓存
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JdbcDaoMethod {
	JdbcMethodEnum methodEnum();
}
