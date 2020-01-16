package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.dao.IDFMissWordDao;
import com.quakoo.framework.ext.recommend.model.IDFMissWord;
import com.quakoo.framework.ext.recommend.service.IDFMissWordService;
import lombok.Data;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class IDFMissWordServiceImpl implements IDFMissWordService {

    @Resource
    private IDFMissWordDao idfMissWordDao;

    @Override
    public void record(Set<String> words) throws Exception {
        idfMissWordDao.record(words);
    }

    @Override
    public List<String> getRecords(int size) throws Exception {
        return idfMissWordDao.getRecords(size);
    }

    @Override
    public void handle(List<IDFMissWord> list) throws Exception {
        List<String> words = Lists.newArrayList();
        for(IDFMissWord one : list) {
            words.add(one.getWord());
        }
        List<IDFMissWord> dbList = idfMissWordDao.load(words);
        Map<String, IDFMissWord> wordMap = Maps.newHashMap();
        for(IDFMissWord one : dbList) {
            wordMap.put(one.getWord(), one);
        }

        List<IDFMissWord> insertList = Lists.newArrayList();
        List<IDFMissWord> replaceList = Lists.newArrayList();
        for(IDFMissWord one : list) {
            if(wordMap.containsKey(one.getWord())) {
                IDFMissWord dbIdfMissWord = wordMap.get(one.getWord());
                dbIdfMissWord.setNum(dbIdfMissWord.getNum() + one.getNum());
                replaceList.add(dbIdfMissWord);
            } else {
                insertList.add(one);
            }
        }
        if(insertList.size() > 0) idfMissWordDao.insert(insertList);
        if(replaceList.size() > 0) idfMissWordDao.replaceNum(replaceList);
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws Exception {
        return idfMissWordDao.getBackPager(pager, word);
    }

}
