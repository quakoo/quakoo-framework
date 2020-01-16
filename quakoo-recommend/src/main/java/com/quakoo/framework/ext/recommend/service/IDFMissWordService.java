package com.quakoo.framework.ext.recommend.service;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.IDFMissWord;

import java.util.List;
import java.util.Set;

public interface IDFMissWordService {

    public void handle(List<IDFMissWord> list) throws Exception;

    public Pager getBackPager(Pager pager, String word) throws Exception;

    public void record(Set<String> words) throws Exception;

    public List<String> getRecords(int size) throws Exception;

}
