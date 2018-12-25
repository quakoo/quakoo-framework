package com.quakoo.space.annotation.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.jdbc.JdbcMethodParamEnum;

/**
 * dao中的方法 标识为只使用数据库
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface JdbcMethodParam {

	JdbcMethodParamEnum paramEnum() default JdbcMethodParamEnum.NULL;

}
