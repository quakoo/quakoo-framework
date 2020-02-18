package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.recommend.bean.Keyword;
import com.quakoo.framework.ext.recommend.dao.HotWordDao;
import com.quakoo.framework.ext.recommend.model.HotWord;
import com.quakoo.framework.ext.recommend.service.HotWordService;
import lombok.Data;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
public class HotWordServiceImpl implements HotWordService {

    @Resource
    private HotWordDao hotWordDao;

    private SimpleDateFormat hourSDF = new SimpleDateFormat("yyyyMMddHH");

    @Override
    public void handle(Date date) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        long timeFormat = Long.parseLong(hourSDF.format(calendar.getTime()));
        List<HotWord> hotWords = hotWordDao.getRecords(timeFormat);
        if(hotWords.size() > 0) {
            Map<String, List<HotWord>> map = Maps.newHashMap();
            for(HotWord hotWord : hotWords) {
                List<HotWord> subList = map.get(hotWord.getWord());
                if(subList == null) {
                    subList = Lists.newArrayList();
                    map.put(hotWord.getWord(), subList);
                }
                subList.add(hotWord);
            }
            hotWords = Lists.newArrayList();
            for(Map.Entry<String, List<HotWord>> entry : map.entrySet()) {
                int num = entry.getValue().size();
                double totalWeight = 0;
                for(HotWord one : entry.getValue()) {
                    totalWeight += one.getWeight();
                }
                BigDecimal bigDecimal = new BigDecimal(totalWeight / num);
                double avgWeight = bigDecimal.setScale(4, RoundingMode.HALF_UP).doubleValue();
                HotWord hotWord = new HotWord();
                hotWord.setWord(entry.getKey());
                hotWord.setWeight(avgWeight);
                hotWord.setNum(num);
                hotWords.add(hotWord);
            }
            Collections.sort(hotWords);
            if(hotWords.size() > 10) hotWords = hotWords.subList(0, 10);
            Collections.reverse(hotWords);
            for(int i = 0; i < hotWords.size(); i++) {
                HotWord hotWord = hotWords.get(i);
                hotWord.setSort(Long.parseLong(timeFormat + "" + i));
            }
            hotWordDao.insert(hotWords);
        }
    }

    @Override
    public List<String> getLastHotWords() throws Exception {
        return hotWordDao.getLastHotWords();
    }

    @Override
    public Pager getBackPager(Pager pager, String word) throws Exception {
        return hotWordDao.getBackPager(pager, word);
    }

    private boolean isChineseWord(String word) {
        String reg = "[\\u4e00-\\u9fa5]+";
        boolean res = word.matches(reg);
        return res;
    }

    public static void main(String[] args) {
        String word = "欧文是。";
        String reg = "[\\u4e00-\\u9fa5]+";
        boolean res = word.matches(reg);
        System.out.println(res);
    }

    @Override
    public void record(List<Keyword> keywords) throws Exception {
        List<HotWord> hotWords = Lists.newArrayList();
        for(Keyword keyword : keywords) {
            if(isChineseWord(keyword.getWord())) {
                HotWord hotWord = new HotWord();
                hotWord.setWord(keyword.getWord());
                hotWord.setWeight(keyword.getTfidfWeight());
                hotWords.add(hotWord);
            }
        }
        if(hotWords.size() > 0) hotWordDao.record(hotWords);
    }

    @Override
    public int insert(List<HotWord> hotWords) throws Exception {
        int res = hotWordDao.insert(hotWords);
        return res;
    }

    @Override
    public boolean delete(long id) throws Exception {
        boolean res = hotWordDao.delete(id);
        return res;
    }
}
