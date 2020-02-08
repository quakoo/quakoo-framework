package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.IDFMissWord;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Set;

public interface IDFMissWordDao {

    public List<IDFMissWord> load(List<String> words) throws DataAccessException;

    public int insert(List<IDFMissWord> list) throws DataAccessException;

    public void replaceNum(List<IDFMissWord> list) throws DataAccessException;

    public void delete(long id) throws DataAccessException;

    public Pager getBackPager(Pager pager, String word) throws DataAccessException;

    public void record(Set<String> words) throws DataAccessException;

    public List<String> getRecords(int size) throws DataAccessException;

}
