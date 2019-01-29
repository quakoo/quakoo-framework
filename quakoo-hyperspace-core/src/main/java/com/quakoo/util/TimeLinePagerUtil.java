package com.quakoo.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.space.annotation.timeline.TimelineSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TimeLinePagerUtil  {


	static Logger logger = LoggerFactory.getLogger(TimeLinePagerUtil.class);

	static Map<Class, Field> pageCursorMap = new HashMap<Class, Field>();

	public static  Pager result2Response(Set set, Pager pager) {
		List all=new ArrayList();
		for(Object obj:set){
			all.add(obj);
		}
		pager.setData(all);
		buildResponse(pager);
		return pager;
	}

	public static  Pager result2Response(List all, Pager pager) {
		pager.setData(all);
		buildResponse(pager);
		return pager;
	}

	protected static String getCursor(Object o) {
		String result = "0";
		Field[] fields = o.getClass().getDeclaredFields();
		Field pageCursorField = pageCursorMap.get(o.getClass());
		if (pageCursorField == null) {
			for (Field one : fields) {
				TimelineSort sign = one.getAnnotation(TimelineSort.class);
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
			result =  pageCursorField.get(o).toString();
		} catch (Exception e) {
			logger.info("PageUtil error!", e);
			throw new RuntimeException("PageUtil error!", e);
		}

		return result;
	}

	protected static   List sub(List list, int start, int end) {
		List result = new ArrayList();
		for (int i = start; i < end; i++) {
			result.add(list.get(i));
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	protected  static  void buildResponse(Pager pager) {
		boolean has = false;
		String preCursor = "0";
		String nextCursor = "0";
		int size = pager.getSize();
		List temp_data = pager.getData();
		if(temp_data == null || temp_data.size() == 0){
			return;
		}
		Object first = temp_data.get(0);
		preCursor = getCursor(first);
		pager.setPreCursor(preCursor);
		if (temp_data.size() > size) {
			has = true;
		} 
		if(has){
			Object last = temp_data.get(size);
			nextCursor = getCursor(last);
			pager.setData(sub(temp_data, 0, size));
			pager.setNextCursor(nextCursor);
		} 
		pager.setCount(pager.getData().size());
		if (has) {
			pager.setHasnext(has);
		}
	}

}
