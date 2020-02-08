package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.dao.StopWordDao;
import com.quakoo.framework.ext.recommend.model.StopWord;
import com.quakoo.framework.ext.recommend.service.StopWordService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Data
public class StopWordServiceImpl implements StopWordService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(StopWordServiceImpl.class);

    @Resource
    private StopWordDao stopWordDao;

    private Set<String> stopWordsSet = Sets.newLinkedHashSet();

    @Override
    public void afterPropertiesSet() throws Exception {
        stopWordsSet = stopWordDao.loadStopWords();
        Thread processer = new Thread(new Processer());
        processer.start();
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MINUTES);
                try {
                    Set<String> set = stopWordDao.loadStopWords();
                    stopWordsSet = set;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public int insert(List<StopWord> stopWords) throws Exception {
        int res = stopWordDao.insert(stopWords);
        return res;
    }

    @Override
    public boolean update(StopWord stopWord) throws Exception {
        boolean res = stopWordDao.update(stopWord);
        return res;
    }

    @Override
    public boolean delete(long id) throws Exception {
        boolean res = stopWordDao.delete(id);
        return res;
    }

    @Override
    public Set<String> loadStopWords() throws Exception {
        return stopWordsSet;
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws Exception {
        return stopWordDao.getBackPager(pager, word);
    }

}
