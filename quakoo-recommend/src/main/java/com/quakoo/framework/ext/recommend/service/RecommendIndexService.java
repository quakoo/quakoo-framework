package com.quakoo.framework.ext.recommend.service;

import java.util.List;

public interface RecommendIndexService {

    public void recordDelIndex(String index, List<String> ids) throws Exception;

    public void handleDelIndex(int size) throws Exception;

}
