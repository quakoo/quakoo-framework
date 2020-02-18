package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.transform.TransformMapUtils;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.Keyword;
import com.quakoo.framework.ext.recommend.bean.PortraitItem;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import com.quakoo.framework.ext.recommend.dao.PortraitItemCFDao;
import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import com.quakoo.framework.ext.recommend.service.PortraitItemCFService;
import com.quakoo.framework.ext.recommend.service.TFIDFService;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class PortraitItemCFServiceImpl implements PortraitItemCFService, InitializingBean {

    @Resource
    private AbstractRecommendInfo recommendInfo;

    @Resource
    private TFIDFService tfidfService;

    @Resource
    private PortraitItemCFDao portraitItemCFDao;

    private JedisX cache;

    private final static String portrait_item_queue_key = "%s_portrait_item_queue";

    private TransformMapUtils mapUtils = new TransformMapUtils(PortraitItemCF.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public PortraitItemCF load(long uid) throws Exception {
        PortraitItemCF res = portraitItemCFDao.load(uid);
        return res;
    }

    private boolean isChineseWord(String word) {
        String reg = "[\\u4e00-\\u9fa5]+";
        boolean res = word.matches(reg);
        return res;
    }

    @Override
    public void record(long uid, String title) throws Exception {
        String key = String.format(portrait_item_queue_key, recommendInfo.projectName);
        List<Keyword> keywords = tfidfService.analyze(title, 5);
        PortraitItem portraitItem = new PortraitItem();
        portraitItem.setUid(uid);
        Map<String, Double> weightMap = Maps.newHashMap();
        for(Keyword keyword : keywords) {
            if(isChineseWord(keyword.getWord())) {
                weightMap.put(keyword.getWord(), keyword.getTfidfWeight());
            }
        }
        portraitItem.setWeightMap(weightMap);
        if(weightMap.size() > 0) cache.rpushObject(key, portraitItem);
    }

    @Override
    public void handle(int size) throws Exception {
        String key = String.format(portrait_item_queue_key, recommendInfo.projectName);
        List<Object> list = cache.lrangeAndDelObject(key, 0, size, null);
        if(list.size() > 0) {
            Map<Long, List<PortraitItem>> map = Maps.newHashMap();
            for(Object obj : list) {
                PortraitItem portraitItem = (PortraitItem)obj;
                List<PortraitItem> portraitItems = map.get(portraitItem.getUid());
                if(portraitItems == null) {
                    portraitItems = Lists.newArrayList();
                    map.put(portraitItem.getUid(), portraitItems);
                }
                portraitItems.add(portraitItem);
            }

            List<PortraitItemCF> portraits = portraitItemCFDao.load(Lists.newArrayList(map.keySet()));
            Map<Long, PortraitItemCF> portraitMap = mapUtils.listToMap(portraits, "uid");

            List<PortraitItemCF> updateList = Lists.newArrayList();
            List<PortraitItemCF> insertList = Lists.newArrayList();
            for(Map.Entry<Long, List<PortraitItem>> entry : map.entrySet()) {
                long uid = entry.getKey();
                Map<String, Double> weightMap = Maps.newHashMap();
                for(PortraitItem one : entry.getValue()) {
                    for(Map.Entry<String, Double> weightMapEntry : one.getWeightMap().entrySet()) {
                        Double weight = weightMap.get(weightMapEntry.getKey());
                        if(weight != null) {
                            weight = weight + weightMapEntry.getValue();
                        } else {
                            weight = weightMapEntry.getValue();
                        }
                        weightMap.put(weightMapEntry.getKey(), weight);
                    }
                }

                if(portraitMap.keySet().contains(uid)) {
                    PortraitItemCF portrait = portraitMap.get(uid);
                    Map<String, Double> itemMap = Maps.newHashMap();
                    for(PortraitWord portraitWord : portrait.getWords()) {
                        itemMap.put(portraitWord.getWord(), portraitWord.getWeight());
                    }

                    for(Map.Entry<String, Double> itemEntry : itemMap.entrySet()) {
                        Double weight = weightMap.get(itemEntry.getKey());
                        if(weight != null) {
                            weight = weight + itemEntry.getValue();
                        } else {
                            weight = itemEntry.getValue();
                        }
                        weightMap.put(itemEntry.getKey(), weight);
                    }

                    List<PortraitWord> portraitWords = Lists.newArrayList();
                    for(Map.Entry<String, Double> weightEntry : weightMap.entrySet()) {
                        PortraitWord portraitWord = new PortraitWord();
                        portraitWord.setWord(weightEntry.getKey());
                        portraitWord.setWeight(weightEntry.getValue());
                        portraitWords.add(portraitWord);
                    }
                    Collections.sort(portraitWords);
                    if(portraitWords.size() > 30) portraitWords = portraitWords.subList(0, 30);
                    portrait.setWords(portraitWords);
                    updateList.add(portrait);
                } else {
                    List<PortraitWord> portraitWords = Lists.newArrayList();
                    for(Map.Entry<String, Double> weightEntry : weightMap.entrySet()) {
                        PortraitWord portraitWord = new PortraitWord();
                        portraitWord.setWord(weightEntry.getKey());
                        portraitWord.setWeight(weightEntry.getValue());
                        portraitWords.add(portraitWord);
                    }
                    Collections.sort(portraitWords);
                    if(portraitWords.size() > 30) portraitWords = portraitWords.subList(0, 30);
                    PortraitItemCF portrait = new PortraitItemCF();
                    portrait.setUid(uid);
                    portrait.setWords(portraitWords);
                    insertList.add(portrait);
                }
            }

            if(updateList.size() > 0) {
                portraitItemCFDao.update(updateList);
            }

            if(insertList.size() > 0) {
                portraitItemCFDao.insert(insertList);
            }
        }
    }

}
