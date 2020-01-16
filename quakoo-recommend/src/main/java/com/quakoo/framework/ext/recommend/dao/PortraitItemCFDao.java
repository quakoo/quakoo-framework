package com.quakoo.framework.ext.recommend.dao;

import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface PortraitItemCFDao {

    public int insert(List<PortraitItemCF> list) throws DataAccessException;

    public List<PortraitItemCF> load(List<Long> uids) throws DataAccessException;

    public PortraitItemCF load(long uid) throws DataAccessException;

    public int update(List<PortraitItemCF> list) throws DataAccessException;

}
