package com.quakoo.space.annotation.timeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * timeline的sort字段 必须是long类型
 * @author LiYongbiao1
 *
 */
public @interface TimelineSort {

}
