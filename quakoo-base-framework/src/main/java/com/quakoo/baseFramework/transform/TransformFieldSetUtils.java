package com.quakoo.baseFramework.transform;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Set;

public class TransformFieldSetUtils {

    private List<PropertyDescriptor> listFieldDescriptors = Lists.newArrayList();

    public TransformFieldSetUtils(Class<?> clazz){
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

    public <F> Set<F> fieldList(List<?> list, String fieldName) {
        PropertyDescriptor listFieldDescriptor = null;
        for(PropertyDescriptor one : listFieldDescriptors) {
            if(one.getName().equals(fieldName)) {
                listFieldDescriptor = one;
                break;
            }
        }
        if(null == listFieldDescriptor) throw new IllegalStateException("TransformFieldListUtils fieldName is error!");
        try {
            Set<F> res = Sets.newLinkedHashSet();
            for(Object one : list) {
                if(null != one) {
                    F value = (F) listFieldDescriptor.getReadMethod().invoke(one);
                    res.add(value);
                }
            }
            return res;
        } catch (Exception e) {
            throw new IllegalStateException("TransformFieldListUtils fieldList is error!", e);
        }
    }

}
