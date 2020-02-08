package com.quakoo.framework.ext.recommend.service;

import com.quakoo.framework.ext.recommend.bean.Keyword;

import java.util.List;

public interface TFIDFService {

    public List<Keyword> analyze(String content, int topN) throws Exception;

}
