package com.quakoo.space.annotation.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.cache.CacheMethodParamEnum;

/**
 * 用以标识CacheDaoMethod中的参数
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CacheMethodParam {
	/**
	 * 参数的类型
	 * @return
	 */
    CacheMethodParamEnum paramEnum() default CacheMethodParamEnum.NULL;

    /**
     * 
     * @return
     */
    String field() default "";
}
