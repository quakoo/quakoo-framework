package com.quakoo.space.annotation.timeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.timeline.TimelineMethodParamEnum;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TimelineMethodParam {
    TimelineMethodParamEnum paramEnum() default TimelineMethodParamEnum.NULL;

    String field() default "";
}
