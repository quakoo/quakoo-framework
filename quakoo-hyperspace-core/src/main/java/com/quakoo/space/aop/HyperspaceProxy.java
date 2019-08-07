package com.quakoo.space.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.quakoo.space.aop.cache.CacheCommonMethodInterceptor;
import com.quakoo.space.aop.index.IndexCommonMethodInterceptor;
import com.quakoo.space.aop.timeline.TimelineCommonMethodInterceptor;
//import net.sf.cglib.proxy.Enhancer;

import com.quakoo.space.aop.jdbc.JdbcCommonMethodInterceptor;
import org.springframework.cglib.proxy.Enhancer;

public class HyperspaceProxy {
	public void copy(Object target, Object source) throws Exception {
		Class<?> clazz = source.getClass();
		while (clazz != null) {
			Field[] sourceFields = clazz.getDeclaredFields();
			for (Field sourceField : sourceFields) {
				boolean isStatic = Modifier
						.isStatic(sourceField.getModifiers());
				boolean isFinal = Modifier.isFinal(sourceField.getModifiers());
				if (!(isStatic && isFinal)) {
					sourceField.setAccessible(true);
					Object sourceValue = sourceField.get(source);
					sourceField.set(target, sourceValue);
				}
			}
			clazz = clazz.getSuperclass();
		}
	}

	public Object createCacheProxy(Class targetClass) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(targetClass);
		enhancer.setCallback(new CacheCommonMethodInterceptor());
		return enhancer.create();
	}

    public Object createIndexProxy(Class targetClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new IndexCommonMethodInterceptor());
        return enhancer.create();
    }

	public Object createJdbcProxy(Class targetClass) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(targetClass);
		enhancer.setCallback(new JdbcCommonMethodInterceptor());
		return enhancer.create();
	}

	public Object createTimelineProxy(Class targetClass) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(targetClass);
		enhancer.setCallback(new TimelineCommonMethodInterceptor());
		return enhancer.create();
	}
}
