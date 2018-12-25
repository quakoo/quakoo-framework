package com.quakoo.baseFramework.transform;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;


public class TransformMapUtils {

    private List<PropertyDescriptor> listFieldDescriptors = Lists.newArrayList();

    public TransformMapUtils(Class<?> clazz){
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
            for(PropertyDescriptor one : propertyDescriptors) {
                String fieldName = one.getName();
                if (!"class".equals(fieldName)) {
                    listFieldDescriptors.add(one);
                }

            }
        } catch (Exception e) {
            throw new IllegalStateException("TransformUtils is error!", e);
        }
    }

    public <K, V> Map<K, V> listToMap(List<V> list, String fieldName) {
        PropertyDescriptor listFieldDescriptor = null;
        for(PropertyDescriptor one : listFieldDescriptors) {
            if(one.getName().equals(fieldName)) {
                listFieldDescriptor = one;
                break;
            }
        }
        if(null == listFieldDescriptor) throw new IllegalStateException("TransformMapUtils fieldName is error!");
        try {
            Map<K, V> res = Maps.newHashMap();
            for(V one : list) {
               if(null != one) {
                   K key = (K) listFieldDescriptor.getReadMethod().invoke(one);
                   res.put(key, one);
               }
            }
            return res;
        } catch (Exception e) {
            throw new IllegalStateException("TransformMapUtils listToMap is error!", e);
        }
    }

}
