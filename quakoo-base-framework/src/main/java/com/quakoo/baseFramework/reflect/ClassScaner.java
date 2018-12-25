package com.quakoo.baseFramework.reflect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liyongbiao 在某个包下，扫描出带annotation的class
 */
@SuppressWarnings("rawtypes")
public class ClassScaner {
	static Logger logger=LoggerFactory.getLogger(ClassScaner.class);
    private static Map<String, List<Class>> cacheClassMap = new HashMap<String, List<Class>>();

    public static Map<Class, Annotation> scanClasses(String packageName, Class annotationClass) {
        if (cacheClassMap.get(packageName) == null) {
            List<String> classNames = ResourceLoader.getClassesInPackage(packageName);
            List<Class> classes = new ArrayList<Class>();
            for (String className : classNames) {
                try {
                    Class clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (Error e) {
                } catch (Exception e) {
                }
            }
            cacheClassMap.put(packageName, classes);
        }
        List<Class> classes = cacheClassMap.get(packageName);
        Map<Class, Annotation> results = new HashMap<Class, Annotation>();
        for (Class clazz : classes) {
            @SuppressWarnings("unchecked")
            Annotation annotation = clazz.getAnnotation(annotationClass);
            if (annotation != null) {
                results.put(clazz, annotation);
            }
        }
        return results;
    }

    public static List<Class> getSubClasses(String packageName, Class superClass) {
        if (cacheClassMap.get(packageName) == null) {
            List<String> classNames = ResourceLoader.getClassesInPackage(packageName);
            List<Class> classes = new ArrayList<Class>();
            for (String className : classNames) {
                try {
                    Class clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (Exception e) {
                }
            }
            cacheClassMap.put(packageName, classes);
        }
        List<Class> classes = cacheClassMap.get(packageName);
        List<Class> results = new ArrayList<Class>();
        for (Class clazz : classes) {
            if (superClass.isAssignableFrom(clazz) && !clazz.equals(superClass)) {
                results.add(clazz);
            }
        }
        return results;
    }

    public static void main(String[] sdg) {
        List<Class> classes = getSubClasses("com.chinaren.framework", java.io.Serializable.class);
        for (Class clazz : classes) {
            System.out.println(clazz.getName());
        }

        System.out.println("----------");
        // classes = getSubClasses("com.chinaren.framework", s);
        for (Class clazz : classes) {
            System.out.println(clazz.getName());
        }

        System.exit(0);
    }

}
