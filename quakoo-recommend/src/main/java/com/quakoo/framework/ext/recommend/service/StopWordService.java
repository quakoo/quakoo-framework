package com.quakoo.framework.ext.recommend.service;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.StopWord;

import java.util.List;
import java.util.Set;

public interface StopWordService {

    public int insert(List<StopWord> stopWords) throws Exception;

    public boolean update(StopWord stopWord) throws Exception;

    public boolean delete(long id) throws Exception;

    public Set<String> loadStopWords() throws Exception;

    public Pager getBackPager(Pager pager, String word) throws Exception;

}
