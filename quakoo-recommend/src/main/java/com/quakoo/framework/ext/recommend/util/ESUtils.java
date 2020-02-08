package com.quakoo.framework.ext.recommend.util;

import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.recommend.bean.ESField;

import java.util.List;
import java.util.Map;

public class ESUtils {

    public static String toIndexJson(List<ESField> fields) {
        Map<String, Map<String, String>> fieldMap = Maps.newHashMap();
        for(ESField one : fields) {
            Map<String, String> propertyMap = Maps.newLinkedHashMap();
            fieldMap.put(one.getName(), propertyMap);
            propertyMap.put("index", one.getIndex());
            propertyMap.put("type", one.getType());
            propertyMap.put("analyzer", one.getAnalyzer());
        }
        Map<String, Map<String, Map<String, String>>> resMap = Maps.newHashMap();
        resMap.put("properties", fieldMap);
        return JsonUtils.toJson(resMap);
    }

}
