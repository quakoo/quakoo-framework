package com.quakoo.space.annotation.index;

import com.quakoo.space.enums.cache.CacheSortOrder;
import com.quakoo.space.enums.index.IndexMethodEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IndexDaoMethod {

    IndexMethodEnum methodEnum();

    CacheSortOrder order() default CacheSortOrder.desc; // 默认为倒序

}
