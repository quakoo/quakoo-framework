package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import com.quakoo.framework.ext.recommend.bean.RecallItem;
import com.quakoo.framework.ext.recommend.bean.SearchRes;
import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import com.quakoo.framework.ext.recommend.service.FilterService;
import com.quakoo.framework.ext.recommend.service.HotWordService;
import com.quakoo.framework.ext.recommend.service.PortraitItemCFService;
import com.quakoo.framework.ext.recommend.service.RecommendService;
import com.quakoo.framework.ext.recommend.service.ext.RealTimeSearchService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Data
public class RecommendServiceImpl implements RecommendService, InitializingBean {

    Logger logger = LoggerFactory.getLogger(RecommendServiceImpl.class);

    @Resource
    private AbstractRecommendInfo recommendInfo;

    @Resource
    private RealTimeSearchService realTimeSearchService;

    @Resource
    private PortraitItemCFService portraitItemCFService;

    @Resource
    private HotWordService hotWordService;

    @Resource
    private FilterService filterService;

    private JedisX cache;

    private String search_time_queue_key = "%s_search_time_queue_uid_%d";
    private String search_hot_word_queue_key = "%s_search_hot_word_queue_uid_%d";
    private String search_item_cf_queue_key = "%s_search_item_cf_queue_uid_%d";

    private String recall_list_key = "%s_recall_list_user_%d";

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    private List<SearchRes> searchByTime(long uid) throws Exception {
        String key = String.format(search_time_queue_key, recommendInfo.projectName, uid);
        Set<Object> set = cache.zrevrangeByScoreObject(key, Double.MAX_VALUE, 0, null);
        if (set.size() > 0) {
            List<SearchRes> res = Lists.newArrayList();
            for (Object obj : set) {
                res.add((SearchRes) obj);
            }
            return res;
        } else {
            List<SearchRes> res = realTimeSearchService.searchByTime(uid);
            if (res.size() > 0) {
                Map<Object, Double> redisMap = Maps.newHashMap();
                for (SearchRes one : res) {
                    redisMap.put(one, (double) one.getTime());
                }
                cache.zaddMultiObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_overtime);
            }
            return res;
        }

    }

    private List<SearchRes> searchByHotWord(long uid) throws Exception {
        String key = String.format(search_hot_word_queue_key, recommendInfo.projectName, uid);
        Set<Object> set = cache.zrevrangeByScoreObject(key, Double.MAX_VALUE, 0, null);
        if (set.size() > 0) {
            List<SearchRes> res = Lists.newArrayList();
            for (Object obj : set) {
                res.add((SearchRes) obj);
            }
            return res;
        } else {
            List<String> hotWords = hotWordService.getLastHotWords();
            if (hotWords.size() == 0) return Lists.newArrayList();
            List<SearchRes> res = realTimeSearchService.search(hotWords, uid);
            if (res.size() > 0) {
                Map<Object, Double> redisMap = Maps.newHashMap();
                for (SearchRes one : res) {
                    redisMap.put(one, one.getScore());
                }
                cache.zaddMultiObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_overtime);
            }
            return res;
        }

    }

    private List<SearchRes> searchByItemCF(long uid) throws Exception {
        String key = String.format(search_item_cf_queue_key, recommendInfo.projectName, uid);
        Set<Object> set = cache.zrevrangeByScoreObject(key, Double.MAX_VALUE, 0, null);
        if (set.size() > 0) {
            List<SearchRes> res = Lists.newArrayList();
            for (Object obj : set) {
                res.add((SearchRes) obj);
            }
            return res;
        } else {
            PortraitItemCF portraitItemCF = portraitItemCFService.load(uid);
            if (portraitItemCF == null) return Lists.newArrayList();
            List<PortraitWord> portraitWords = portraitItemCF.getWords();
            List<String> words = Lists.newArrayList();
            int num = portraitWords.size();
            if (num > 10) num = 10;
            for (int i = 0; i < num; i++) {
                PortraitWord portraitWord = portraitWords.get(i);
                words.add(portraitWord.getWord());
            }
            List<SearchRes> res = realTimeSearchService.search(words, uid);
            if (res.size() > 0) {
                Map<Object, Double> redisMap = Maps.newHashMap();
                for (SearchRes one : res) {
                    redisMap.put(one, one.getScore());
                }
                cache.zaddMultiObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_overtime);
            }
            return res;
        }
    }

    private List<RecallItem> filter(long uid, int type, List<SearchRes> list) throws Exception {
        if (list.size() == 0) return Lists.newArrayList();
        List<Long> aids = Lists.newArrayList();
        for (SearchRes one : list) {
            aids.add(one.getId());
        }
        long startTime = System.currentTimeMillis();
        Map<Long, Boolean> existsMap = filterService.filter(uid, type, aids);
        logger.info("filter uid : " + uid + ", type " + type + ", time : " + (System.currentTimeMillis() - startTime));
        List<RecallItem> res = Lists.newArrayList();
        for (SearchRes one : list) {
            Boolean exists = existsMap.get(one.getId());
            if (exists != null && !exists) {
                RecallItem recallItem = new RecallItem(one.getId(), type);
                res.add(recallItem);
            }
        }
        //如果没有新的数据则 清除过滤器重新推荐
        if (res.size() == 0) {
            filterService.clear(uid, type);
            for (SearchRes one : list) {
                RecallItem recallItem = new RecallItem(one.getId(), type);
                res.add(recallItem);
            }
        }
        return res;
    }

//    private List<RecallItem> recallTime(long uid) throws Exception {
//        List<SearchRes> searchResList = searchByTime(uid);
//        List<RecallItem> recallItems = filter(uid, AbstractRecommendInfo.type_time, searchResList);
//        return recallItems;
//    }
//
//    private List<RecallItem> recallHotWord(long uid) throws Exception {
//        List<SearchRes> searchResList = searchByHotWord(uid);
//        List<RecallItem> recallItems = filter(uid, AbstractRecommendInfo.type_hot_word, searchResList);
//        return recallItems;
//    }
//
//    private List<RecallItem> recallItemCF(long uid) throws Exception {
//        List<SearchRes> searchResList = searchItemCF(uid);
//        List<RecallItem> recallItems = filter(uid, AbstractRecommendInfo.type_item_cf, searchResList);
//        return recallItems;
//    }

    private Map<Integer, List<RecallItem>> recall(long uid) throws Exception {
        CompletionService<List<RecallItem>> completionService = new ExecutorCompletionService<>(recommendInfo.executorService);
        Map<Integer, List<RecallItem>> resMap = Maps.newHashMap();
        for(int type = 1; type <= 3; type++) {
            completionService.submit(new RecallProcesser(type, uid));
        }
        List<RecallItem> res = Lists.newArrayList();
        for (int i = 0; i < 3; i++) {
            List<RecallItem> oneRes = completionService.take().get();
            res.addAll(oneRes);
        }
        for(RecallItem one : res) {
            List<RecallItem> subList = resMap.get(one.getType());
            if(subList == null) {
                subList = Lists.newArrayList();
                resMap.put(one.getType(), subList);
            }
            subList.add(one);
        }
        return resMap;
    }

    class RecallProcesser implements Callable<List<RecallItem>> {
        private int type;
        private long uid;
        public RecallProcesser(int type, long uid) {
            this.type = type;
            this.uid = uid;
        }
        @Override
        public List<RecallItem> call() throws Exception {
            List<SearchRes> searchResList = null;
            if(type == AbstractRecommendInfo.type_time) searchResList = searchByTime(uid);
            else if(type == AbstractRecommendInfo.type_hot_word) searchResList = searchByHotWord(uid);
            else searchResList = searchByItemCF(uid);
            List<RecallItem> recallItems = filter(uid, type, searchResList);
            return recallItems;
        }
    }


    private void initRecall(long uid) throws Exception {
        int item_cf_step = recommendInfo.itemCFStep;
        int hot_word_step = recommendInfo.hotWordStep;
        int time_step = recommendInfo.timeStep;
        int cache_multiple = recommendInfo.cacheMultiple;
        String key = String.format(recall_list_key, recommendInfo.projectName, uid);
        if (!cache.exists(key)) {
            Map<Integer, List<RecallItem>> recallItemsMap = recall(uid);
            List<RecallItem> standbyList = Lists.newArrayList();

            List<RecallItem> itemCFs = recallItemsMap.get(AbstractRecommendInfo.type_item_cf);
            if(itemCFs == null) itemCFs = Lists.newArrayList();
            int itemCFLen = itemCFs.size();
            if (itemCFLen > item_cf_step * cache_multiple) itemCFLen = item_cf_step * cache_multiple;
            itemCFs = itemCFs.subList(0, itemCFLen);
            List<List<RecallItem>> itemCFsList = Lists.partition(itemCFs, item_cf_step);

            List<RecallItem> hotwords = recallItemsMap.get(AbstractRecommendInfo.type_hot_word);
            if(hotwords == null) hotwords = Lists.newArrayList();
            int hotwordLen = hotwords.size();
            if (hotwordLen > hot_word_step * cache_multiple) hotwordLen = hot_word_step * cache_multiple;
            List<RecallItem> thisHotwords = hotwords.subList(0, hotwordLen);
            List<List<RecallItem>> hotwordsList = Lists.partition(thisHotwords, hot_word_step);
            if (hotwordLen == hot_word_step * cache_multiple) {
                List<RecallItem> otherHotwords = hotwords.subList(hotwordLen, hotwords.size());
                if (otherHotwords.size() > (item_cf_step + hot_word_step + time_step) * cache_multiple) {
                    otherHotwords = otherHotwords.subList(0, (item_cf_step + hot_word_step + time_step) * cache_multiple);
                }
                if (otherHotwords.size() > 0) standbyList.addAll(otherHotwords);
            }

            List<RecallItem> times = recallItemsMap.get(AbstractRecommendInfo.type_time);
            if(times == null) times = Lists.newArrayList();
            int timeLen = times.size();
            if (timeLen > time_step * cache_multiple) timeLen = time_step * cache_multiple;
            List<RecallItem> thisTimes = times.subList(0, timeLen);
            List<List<RecallItem>> timesList = Lists.partition(thisTimes, time_step);
            if (timeLen == time_step * cache_multiple) {
                List<RecallItem> otherTimes = times.subList(timeLen, times.size());
                if (otherTimes.size() > (item_cf_step + hot_word_step + time_step) * cache_multiple) {
                    otherTimes = otherTimes.subList(0, (item_cf_step + hot_word_step + time_step) * cache_multiple);
                }
                if (otherTimes.size() > 0) standbyList.addAll(otherTimes);
            }
            Collections.shuffle(standbyList);

            List<Object> redisList = Lists.newArrayList();
            for (int i = 0; i < cache_multiple; i++) {
                List<RecallItem> list = Lists.newArrayList();
                if(itemCFsList.size() > i) {
                    List<RecallItem> one_itemCFs = itemCFsList.get(i);
                    if (one_itemCFs != null) list.addAll(one_itemCFs);
                }
                if(hotwordsList.size() > i) {
                    List<RecallItem> one_hotwords = hotwordsList.get(i);
                    if (one_hotwords != null) list.addAll(one_hotwords);
                }
                if(timesList.size() > i) {
                    List<RecallItem> one_times = timesList.get(i);
                    if (one_times != null) list.addAll(one_times);
                }

                if (list.size() < (item_cf_step + hot_word_step + time_step)) {
                    int standbyNum = item_cf_step + hot_word_step + time_step - list.size();
                    if (standbyList.size() >= standbyNum) {
                        List<RecallItem> one_standbyList = standbyList.subList(0, standbyNum);
                        list.addAll(one_standbyList);
                        standbyList.removeAll(one_standbyList);
                    }
                }
                Collections.shuffle(list);
                redisList.add(list);
            }
            cache.piprpushObject(key, redisList);
            cache.expire(key, AbstractRecommendInfo.redis_overtime);
        }
    }

    @Override
    public List<Long> recommend(long uid) throws Exception {
        long startTime = System.currentTimeMillis();
        initRecall(uid);
        String key = String.format(recall_list_key, recommendInfo.projectName, uid);
        List<Object> objList = cache.lrangeAndDelObject(key, 0, 0, null);
        List<RecallItem> recallItems = (List<RecallItem>) objList.get(0);
        Set<Long> idSet = Sets.newLinkedHashSet();
        Map<Integer, List<Long>> recordsMap = Maps.newHashMap();
        for (RecallItem recallItem : recallItems) {
            idSet.add(recallItem.getId());
            List<Long> records = recordsMap.get(recallItem.getType());
            if (records == null) {
                records = Lists.newArrayList();
                recordsMap.put(recallItem.getType(), records);
            }
//            System.out.println("type : " + recallItem.getType() + ", id : " + recallItem.getId());
            records.add(recallItem.getId());
        }
        for (Map.Entry<Integer, List<Long>> entry : recordsMap.entrySet()) {
            filterService.record(uid, entry.getKey(), entry.getValue());
        }
        logger.info("======= recommend uid : " + uid + ",time : " + (System.currentTimeMillis() - startTime));
        return Lists.newArrayList(idSet);
    }

    @Override
    public void record(long uid, String title) throws Exception {
        portraitItemCFService.record(uid, title);
    }

    public static void main(String[] args) {
//        List<Integer> standbyList = Lists.newArrayList();
//        List<Integer> list = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
//        int num = 2 * 3;
//        List<Integer> thisList = list.subList(0, num);
//        List<Integer> otherList = list.subList(num, list.size());
//        standbyList.addAll(otherList);
//
//        List<List<Integer>> lists = Lists.partition(thisList, 2);
//        for (int i = 0; i < 3; i++) {
//            List<Integer> aa = Lists.newArrayList();
//            aa.addAll(lists.get(i));
//            int standbyNum = 2;
//            if (standbyList.size() >= standbyNum) {
//                List<Integer> one_standbyList = standbyList.subList(0, standbyNum);
//                aa.addAll(one_standbyList);
//                standbyList.removeAll(one_standbyList);
//            }
//            System.out.println(aa.toString());
//        }
        List<Long> list = Lists.newArrayList();
        list.subList(0, 0);
        System.out.println(list.toString());
    }

}
