package com.quakoo.space;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.quakoo.baseFramework.thread.NamedThreadFactory;

public class MqBaseDao<T> extends AbstractCacheBaseDao<T> {

    ExecutorService pool = Executors.newCachedThreadPool(new NamedThreadFactory("mqBaseDao"));

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
    public void afterPropertiesSet() throws Exception {

    }

	@Override
	protected void afterIncrement(T oldModel, T newModel) {
	}

}
