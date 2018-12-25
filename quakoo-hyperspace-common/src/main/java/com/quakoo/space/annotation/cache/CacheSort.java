package com.quakoo.space.annotation.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.cache.CacheSortOrder;

/**
 * 
 * 标准这个方法取的是正序还是倒序。
 * @author LiYongbiao1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheSort {

    CacheSortOrder order() default CacheSortOrder.desc; // 默认为倒序

}
