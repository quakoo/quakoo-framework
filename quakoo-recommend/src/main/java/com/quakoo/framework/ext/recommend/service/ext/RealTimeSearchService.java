package com.quakoo.framework.ext.recommend.service.ext;

import com.quakoo.framework.ext.recommend.bean.SearchRes;

import java.util.List;

public interface RealTimeSearchService {

    public List<SearchRes> search(List<String> words, long uid) throws Exception;

    public List<SearchRes> searchByTime(long uid) throws Exception;


}
