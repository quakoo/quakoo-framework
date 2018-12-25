package com.quakoo.baseFramework.reflect;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import com.quakoo.baseFramework.property.PropertyLoader;

/**
 * 
 * @author liyongbiao
 *
 */
public class Property2Obj {

    public static Object property2Obj(Object obj, PropertyLoader configurator) throws Exception {

        PropertyDescriptor[] pds = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();

        for (PropertyDescriptor pd : pds) {
            if (!"class".equals(pd.getName())) {
                Class clazz = pd.getPropertyType();
                if (clazz.equals(int.class)) {
                    pd.getWriteMethod().invoke(obj, configurator.getIntProperty(pd.getName()));
                } else if (clazz.equals(boolean.class)) {
                    pd.getWriteMethod().invoke(obj, configurator.getBooleanProperty(pd.getName()));
                } else if (clazz.equals(String.class)) {
                    pd.getWriteMethod().invoke(obj, configurator.getProperty(pd.getName()));
                } else {
                    throw new RuntimeException("not support type:" + clazz);
                }

            }
        }
        return obj;
    }

    public static void main(String[] args) {

    }
}
