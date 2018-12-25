package com.quakoo.space.annotation.timeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * 去重的字段
 * @author LiYongbiao1
 *
 */
public @interface TimelineRepetFields {

	/**
	 * 如果需要联合去重，确保为同一id<br>
	 * 如：我们要去重同一vid。<br>
	 * 在vid上加上注解@FilterByEquals(group=0)<br>
	 * 还要去重title<br>
	 * 在title上加上注解@FilterByEquals(group=1)<br>
	 * 还要去重site和siteID<br>
	 * 在site上和siteID上同时加上注解@FilterByEquals(group=2)<br>
	 *
	 * @return
	 */
    int group() ;
}
