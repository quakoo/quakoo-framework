package com.quakoo.baseFramework.excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelInfo {

    ExcelReadType readType();

    int readRow() default 0;

    int readCol() default 0;

}
