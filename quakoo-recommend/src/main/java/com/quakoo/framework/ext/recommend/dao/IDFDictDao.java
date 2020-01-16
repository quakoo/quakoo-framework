package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.IDFDict;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;

public interface IDFDictDao {

    public int insert(List<IDFDict> idfDicts) throws DataAccessException;

    public boolean update(IDFDict idfDict) throws DataAccessException;

    public boolean delete(long id) throws DataAccessException;

    public Map<String, Double> loadIDFMap() throws DataAccessException;

    public Pager getBackPager(Pager pager, String word) throws DataAccessException;

}
