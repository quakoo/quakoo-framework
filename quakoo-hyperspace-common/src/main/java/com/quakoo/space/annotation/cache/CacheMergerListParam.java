package com.quakoo.space.annotation.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在聚合list的方法中，标识某个参数为需要聚合的参数。<br>
 * 详细用法参考例子里的MergeList
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CacheMergerListParam {
}
