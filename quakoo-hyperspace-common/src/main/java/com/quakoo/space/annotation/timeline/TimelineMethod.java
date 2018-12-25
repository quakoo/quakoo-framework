package com.quakoo.space.annotation.timeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.timeline.TimelineMethodEnum;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TimelineMethod {
    TimelineMethodEnum methodEnum();
    /**
     * timeline的类型。<br>
     * 类型一致的时候，去读unread数目和去list ，参数要一致
     *
     * @return
     */
    int	timelineType();

}
