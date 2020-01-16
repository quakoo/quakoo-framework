package com.quakoo.framework.ext.recommend.service;

import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.model.IDFDict;
import java.util.List;
import java.util.Map;

public interface IDFDictService {

    public int insert(List<IDFDict> idfDicts) throws Exception;

    public boolean update(IDFDict idfDict) throws Exception;

    public boolean delete(long id) throws Exception;

    public Map<String, Double> loadIDFMap() throws Exception;

    public double getMedianWeight() throws Exception;

    public Pager getBackPager(Pager pager, String word) throws Exception;

}
