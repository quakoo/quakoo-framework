package com.quakoo.space.annotation.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 联合组建。 联合组建和主键可以并存。 一般list都会有联合组建，用来快速通过联合组建，来定位一条数据。
 * 如果标识了联合组建，那么所有的缓存数据会存两份，一份通过主键来确定一个对象，一份通过联合组建来确定一个对象。
 * 
 * @author LiYongbiao1
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CombinationKey {

}
