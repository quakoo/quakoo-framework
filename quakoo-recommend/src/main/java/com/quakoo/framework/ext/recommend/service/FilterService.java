package com.quakoo.framework.ext.recommend.service;

import java.util.List;
import java.util.Map;

public interface FilterService {

    /**
     * 记录已推荐的文章ID
     * method_name: insertRecommended
     * params: [uid, aids]
     * return: void
     * creat_user: lihao
     * creat_date: 2020/1/10
     * creat_time: 16:33
     **/
    public void record(long uid, int type, List<Long> aids) throws Exception;


    /**
     * 过滤掉已推荐的文章ID
     * method_name: filterRecommended
     * params: [uid, aids]
     * return: java.util.List<java.lang.Long>
     * creat_user: lihao
     * creat_date: 2020/1/10
     * creat_time: 16:39
     **/
    public Map<Long, Boolean> filter(long uid, int type, List<Long> aids) throws Exception;

    /**
     * 清除历史记录
     * method_name: clear
     * params: [uid, type]
     * return: void
     * creat_user: lihao
     * creat_date: 2020/1/14
     * creat_time: 17:58
     **/
    public void clear(long uid, int type) throws Exception;

}
