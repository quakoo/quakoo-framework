package com.quakoo.framework.ext.recommend.service;

import com.quakoo.framework.ext.recommend.model.PortraitItemCF;

public interface PortraitItemCFService {

    public void record(long uid, String title) throws Exception;

    public void handle(int size) throws Exception;


    public PortraitItemCF load(long uid) throws Exception;

}
