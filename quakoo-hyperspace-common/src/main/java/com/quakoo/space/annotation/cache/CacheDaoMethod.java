package com.quakoo.space.annotation.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.cache.CacheMethodEnum;

/**
 * 标识为一个具有缓存功能的dao方法。方法上加上这个标识会开启缓存功能。
 * @author LiYongbiao1
 *	
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheDaoMethod {
	/**
	 * 方法的类别。
	 * @return
	 */
    CacheMethodEnum methodEnum();
    
    /**
     * 关联的方法参数。如果methodEnum是获取聚合或的list结构，需要填写这个关联的方法。<br>
     * 如：详细用法参考例子里的MergeList
     */
    Class[] relationMethodParams() default {};
    
    /**
     * 关联的方法名字。如果methodEnum是获取聚合或的list结构，需要填写这个关联的方法。<br>
     * 如：详细用法参考例子里的MergeList
     */
    String relationMethodName() default "";
}
