package com.quakoo.baseFramework.serialize;


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
public @interface SerializableProperty {

	/**
	 * 类型
	 */
	Type type();

	/**
	 * 序号
	 *
	 */
	int index();

	/**
	 * 是否是数组(特殊类型：byte[] isArray=false。 byte[][] isArray=trye)
	 * @return
	 */
	boolean isArray() default  false;

	/**
	 * 是否是list
	 * @return
	 */
	boolean isList() default false;
	
	/**
	 * pojo的类型(如果不写，list会取泛型)
	 * @return
	 */
	Class pojoClass() default ScloudSerializable.class;
	
}
