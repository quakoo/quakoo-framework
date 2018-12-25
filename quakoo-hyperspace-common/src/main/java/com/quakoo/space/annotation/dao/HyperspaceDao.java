package com.quakoo.space.annotation.dao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.HyperspaceType;

/**
 * 表示dao的类型。
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HyperspaceDao {
    /**
     * dao类型
     * 
     * @return
     */
    HyperspaceType type() default HyperspaceType.cache;
}
