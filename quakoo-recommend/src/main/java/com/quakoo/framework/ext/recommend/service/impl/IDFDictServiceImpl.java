package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.dao.IDFDictDao;
import com.quakoo.framework.ext.recommend.model.IDFDict;
import com.quakoo.framework.ext.recommend.service.IDFDictService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class IDFDictServiceImpl implements IDFDictService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(IDFDictServiceImpl.class);

    @Resource
    private IDFDictDao idfDictDao;

    private Map<String, Double> idfMap = Maps.newHashMap();
    private double idfMedian = 0.1;

    @Override
    public void afterPropertiesSet() throws Exception {
        idfMap = idfDictDao.loadIDFMap();
        if (idfMap.size() > 0) {
            List<Double> weightList = Lists.newArrayList(idfMap.values());
            Collections.sort(weightList);
            idfMedian = weightList.get(weightList.size() / 2);
        }
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MINUTES);
                try {
                    Map<String, Double> map = idfDictDao.loadIDFMap();
                    idfMap = map;
                    if (map.size() > 0) {
                        List<Double> weightList = Lists.newArrayList(map.values());
                        Collections.sort(weightList);
                        idfMedian = weightList.get(weightList.size() / 2);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws Exception {
        return idfDictDao.getBackPager(pager, word);
    }

    @Override
    public int insert(List<IDFDict> idfDicts) throws Exception {
        int res = idfDictDao.insert(idfDicts);
        return res;
    }

    @Override
    public boolean update(IDFDict idfDict) throws Exception {
        boolean res = idfDictDao.update(idfDict);
        return res;
    }

    @Override
    public boolean delete(long id) throws Exception {
        boolean res = idfDictDao.delete(id);
        return res;
    }

    @Override
    public Map<String, Double> loadIDFMap() throws Exception {
        return idfMap;
    }

    @Override
    public double getMedianWeight() throws Exception {
        return idfMedian;
    }

}
