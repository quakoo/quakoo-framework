package com.quakoo.baseFramework.reflect;


/**
 * 
 * @author LiYongbiao
 *
 */
public class ClassloadUtil {

	public static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassloadUtil.class.getClassLoader();
        }
        return classLoader;
    }
}
