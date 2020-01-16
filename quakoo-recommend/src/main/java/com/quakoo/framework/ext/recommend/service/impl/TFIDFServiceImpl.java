package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.framework.ext.recommend.analysis.jieba.JiebaSegmenter;
import com.quakoo.framework.ext.recommend.bean.Keyword;
import com.quakoo.framework.ext.recommend.service.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Data
public class TFIDFServiceImpl implements TFIDFService {

    @Resource
    private StopWordService stopWordService;

    @Resource
    private IDFDictService idfDictService;

    @Resource
    private IDFMissWordService idfMissWordService;

    @Resource
    private HotWordService hotWordService;


    private JiebaSegmenter jiebaSegmenter = JiebaSegmenter.getInstance();

    @Override
    public List<Keyword> analyze(String content, int topN) throws Exception {
        Set<String> missSet = Sets.newLinkedHashSet();
        List<Keyword> keywordList = Lists.newArrayList();
        try {
            if(StringUtils.isBlank(content)) return Lists.newArrayList();
            Map<String, Double> tfMap = getTF(content);
            if(tfMap.size() == 0) return Lists.newArrayList();
            Map<String, Double> idfMap = idfDictService.loadIDFMap();
            double medianWeight = idfDictService.getMedianWeight();

            for(String word:tfMap.keySet()) {
                // 若该词不在idf文档中，则使用平均的idf值(可能定期需要对新出现的网络词语进行纳入)
                if(idfMap.containsKey(word)) keywordList.add(new Keyword(word,idfMap.get(word) * tfMap.get(word)));
                else {
                    keywordList.add(new Keyword(word,medianWeight * tfMap.get(word)));
                    missSet.add(word);
                }
            }
            Collections.sort(keywordList);
            if(keywordList.size() > topN) {
                keywordList = keywordList.subList(0, topN);
            }
            return keywordList;
        } finally {
            if(missSet.size() > 0) {
                try {
                    idfMissWordService.record(missSet);
                } catch (Exception ignored) {}
            }
            if(keywordList.size() > 0) {
                try {
                    hotWordService.record(keywordList);
                } catch (Exception ignored) {}
            }
        }
    }

    private Map<String, Double> getTF(String content) throws Exception {
        Map<String, Double> tfMap = Maps.newHashMap();
        if(StringUtils.isBlank(content)) return tfMap;
        Set<String> stopWords = stopWordService.loadStopWords();
        List<String> segments = jiebaSegmenter.sentenceProcess(content);
        Map<String, Integer> freqMap = Maps.newHashMap();
        int wordSum=0;
        for(String segment:segments) {
            //停用词不予考虑，单字词不予考虑
            if(!stopWords.contains(segment) && segment.length()>1) {
                wordSum++;
                if(freqMap.containsKey(segment)) freqMap.put(segment, freqMap.get(segment) + 1);
                else freqMap.put(segment, 1);
            }
        }
        // 计算double型的tf值
        for(String word : freqMap.keySet()) {
            tfMap.put(word, freqMap.get(word) * 0.1 / wordSum);
        }
        return tfMap;
    }

}
