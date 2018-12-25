package com.quakoo.baseFramework.config;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 
 * @author LiYongbiao
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface ConfigProperty {

	/**
	 * zk path
	 * @return
	 */
	String path();
	
	/**
	 * 是否是json
	 * @return
	 */
	boolean json() default false;

    Class<? extends TypeReferenceModel> typeReference() default DefaultTypeReferenceModel.class ;	
	
}
