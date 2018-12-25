package com.quakoo.space;

public class CacheBaseDao<T> extends AbstractCacheBaseDao<T> {

    @Override
    protected void afterInsert(T model) {

    }

    @Override
    protected void afterUpteDate(T oldModel, T newModel) {

    }

    @Override
    protected void afterDelete(T model) {

    }

	@Override
	protected void afterIncrement(T oldModel, T newModel) {
		
	}

}
