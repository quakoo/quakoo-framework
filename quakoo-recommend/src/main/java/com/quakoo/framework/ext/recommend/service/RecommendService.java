package com.quakoo.framework.ext.recommend.service;

import java.util.List;

public interface RecommendService {

    public List<Long> recommend(long uid) throws Exception;

    public void record(long uid, String title) throws Exception;

}
