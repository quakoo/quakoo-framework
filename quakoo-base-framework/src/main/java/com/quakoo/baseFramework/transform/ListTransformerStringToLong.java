package com.quakoo.baseFramework.transform;

public class ListTransformerStringToLong implements ListTransformer<String, Long>{

	@Override
	public Long transform(String s) {
		return Long.parseLong(s);
	}

}
