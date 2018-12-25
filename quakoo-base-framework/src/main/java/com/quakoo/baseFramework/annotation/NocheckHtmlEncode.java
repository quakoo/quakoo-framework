package com.quakoo.baseFramework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * kongdepeng
 * 2016年2月2日 下午3:43:29
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
//将不需要HTML encode的字段String字段打上该标记(仅仅需要对String类型的做处理)
public @interface NocheckHtmlEncode {

}