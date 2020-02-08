package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.HotWord;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface HotWordDao {

    public void record(List<HotWord> hotWords) throws DataAccessException;

    public List<HotWord> getRecords(long timeFormat) throws DataAccessException;

    public int insert(List<HotWord> list) throws DataAccessException;

    public boolean delete(long id) throws DataAccessException;


    public List<String> getLastHotWords() throws DataAccessException;

    public Pager getBackPager(Pager pager, String word) throws DataAccessException;

}
