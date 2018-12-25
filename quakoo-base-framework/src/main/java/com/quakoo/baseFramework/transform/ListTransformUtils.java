package com.quakoo.baseFramework.transform;

import java.util.List;

import com.google.common.collect.Lists;

public class ListTransformUtils {

	public static <S, T> List<T> transformedList(List<S> list, ListTransformer<S, T> trans) {
	     List<T> res = Lists.newArrayList();
	     for(S one : list) {
	    	 T t = trans.transform(one);
	    	 res.add(t);
	     }
	     return res;
	}

}
