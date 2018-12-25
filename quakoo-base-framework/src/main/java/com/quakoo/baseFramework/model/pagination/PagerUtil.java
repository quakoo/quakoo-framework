package com.quakoo.baseFramework.model.pagination;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagerUtil {

    static Logger logger = LoggerFactory.getLogger(PagerUtil.class);

    static Map<Class, Field> pageCursorMap = new HashMap<Class, Field>();

//    public static Pager result2Response(List all, Pager pager, boolean include) {
//        pager.setData(all);
//        buildResponse(pager, include);
//        return pager;
//    }

    public static String getCursor(Object o, String sortName) {
        String result = "0";
        List<Field> fields = new ArrayList<Field>();
        Class<?> superClass = o.getClass();
        while (superClass != null) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        Field pageCursorField = null;
        for (Field one : fields) {
            PagerCursor sign = one.getAnnotation(PagerCursor.class);
            if (null != sign) {
                if(sortName.equals(sign.sortName())) {
                    one.setAccessible(true);
                    pageCursorField = one;
                    break;
                }
            }
        }
        if (pageCursorField == null) {
            throw new IllegalAccessError("Annotation PageCursor is not find");
        }
        try {
            result = pageCursorField.get(o).toString();
        } catch (Exception e) {
            logger.info("PageUtil error!", e);
            throw new RuntimeException("PageUtil error!", e);
        }
        return result;
    }

    public static String getCursor(Object o) {
        String result = "0";
        List<Field> fields = new ArrayList<Field>();
        Class<?> superClass = o.getClass();
        while (superClass != null) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        // Field[] fields = o.getClass().getDeclaredFields();
        Field pageCursorField = pageCursorMap.get(o.getClass());
        if (pageCursorField == null) {
            for (Field one : fields) {
                PagerCursor sign = one.getAnnotation(PagerCursor.class);
                if (null != sign) {
                    one.setAccessible(true);
                    pageCursorMap.put(o.getClass(), one);
                    pageCursorField = one;
                    break;
                }
            }
        }
        if (pageCursorField == null) {
            throw new IllegalAccessError("Annotation PageCursor is not find");
        }
        try {
            result = pageCursorField.get(o).toString();
        } catch (Exception e) {
            logger.info("PageUtil error!", e);
            throw new RuntimeException("PageUtil error!", e);
        }
        return result;
    }

    public static List sub(List list, int start, int end) {
        List result = new ArrayList();
        for (int i = start; i < end; i++) {
            result.add(list.get(i));
        }
        return result;
    }

//    @SuppressWarnings("rawtypes")
//    protected static void buildResponse(Pager pager, boolean include) {
//        boolean has = false;
//        String preCursor = "0";
//        String nextCursor = "0";
//        int size = pager.getSize();
//        if(include){
//        	 if(size == Integer.MAX_VALUE)
//             	size--;
//             size++;
//        }
//        int sign = 0;
//        List temp_data = pager.getData();
//        if(include && temp_data.size() >= size){
//        	 has = true;
//             sign = size;
//        }else if(!include && temp_data.size() > size){
//        	has = true;
//            sign = size;
//        } else {
//            sign = temp_data.size();
//        }
//        List data = sub(temp_data, 0, sign);
//        if (data != null && data.size() > 0) {
//            Object first = data.get(0);
//            preCursor = getCursor(first);
//            Object last = data.get(data.size() - 1);
//            nextCursor = getCursor(last);
//        }
//        if (include && has)
//            data = sub(data, 0, sign - 1);
//        pager.setData(data);
//        if (has)
//            pager.setNextCursor(nextCursor);
//        pager.setPreCursor(preCursor);
//        pager.setCount(data.size());
//        if (has) {
//            pager.setHasnext(has);
//        }
//    }

}
