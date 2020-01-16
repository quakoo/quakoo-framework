package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.StopWord;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Set;

public interface StopWordDao {

    public int insert(List<StopWord> stopWords) throws DataAccessException;

    public boolean update(StopWord stopWord) throws DataAccessException;

    public boolean delete(long id) throws DataAccessException;

    public Set<String> loadStopWords() throws DataAccessException;

    public Pager getBackPager(Pager pager, String word) throws DataAccessException;

}
