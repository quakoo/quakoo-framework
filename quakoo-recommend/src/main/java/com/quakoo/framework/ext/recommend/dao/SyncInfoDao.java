package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.framework.ext.recommend.model.SyncInfo;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;


public interface SyncInfoDao {

    public boolean insert(SyncInfo syncInfo) throws DataAccessException;

    public boolean updateTrackingValue(long id, long trackingValue) throws DataAccessException;

    public boolean delete(long id) throws DataAccessException;

    public List<SyncInfo> getSyncInfos() throws DataAccessException;



    public List<Map<String, Object>> syncList(SyncInfo syncInfo) throws DataAccessException;

}
