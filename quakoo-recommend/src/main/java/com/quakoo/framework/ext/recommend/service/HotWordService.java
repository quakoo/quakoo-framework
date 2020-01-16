package com.quakoo.framework.ext.recommend.service;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.bean.Keyword;
import com.quakoo.framework.ext.recommend.model.HotWord;

import java.util.Date;
import java.util.List;

public interface HotWordService {

    public void handle(Date date) throws Exception;

    public List<String> getLastHotWords() throws Exception;

    public Pager getBackPager(Pager pager, String word) throws Exception;

    public void record(List<Keyword> keywords) throws Exception;

    public int insert(List<HotWord> hotWords) throws Exception;

    public boolean delete(long id) throws Exception;

}
