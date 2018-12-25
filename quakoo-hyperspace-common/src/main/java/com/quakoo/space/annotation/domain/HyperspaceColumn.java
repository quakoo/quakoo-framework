package com.quakoo.space.annotation.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * domain的属性的标识
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HyperspaceColumn {

    /**
     * 是否映射数据库
     * 
     * @return
     */
    boolean isDbColumn() default true;

    /**
     * 数据库的字段名，如果名字一样 这个可以不填。
     * 
     * @return
     */
    String column() default "";

}
