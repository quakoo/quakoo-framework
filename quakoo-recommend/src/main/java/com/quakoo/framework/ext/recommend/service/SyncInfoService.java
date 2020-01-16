package com.quakoo.framework.ext.recommend.service;

import com.quakoo.framework.ext.recommend.model.SyncInfo;

import java.util.List;

public interface SyncInfoService {

    public boolean insert(SyncInfo syncInfo) throws Exception;

    public boolean updateTrackingValue(long id, long trackingValue) throws Exception;

    public boolean delete(long id) throws Exception;

    public List<SyncInfo> getSyncInfos() throws Exception;

    public int handle(SyncInfo syncInfo) throws Exception;

}
