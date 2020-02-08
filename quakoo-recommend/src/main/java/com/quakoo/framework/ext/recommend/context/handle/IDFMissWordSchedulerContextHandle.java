package com.quakoo.framework.ext.recommend.context.handle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.framework.ext.recommend.distributed.DistributedConfig;
import com.quakoo.framework.ext.recommend.model.IDFMissWord;
import com.quakoo.framework.ext.recommend.service.IDFMissWordService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class IDFMissWordSchedulerContextHandle extends BaseContextHandle {

    Logger logger = LoggerFactory.getLogger(IDFMissWordSchedulerContextHandle.class);

    private int handle_size = 50;

    @Resource
    private IDFMissWordService idfMissWordService;


    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(new Processer());
        thread.start();
    }

    class Processer implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (DistributedConfig.canRunIDFMissWord) {
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MINUTES);
                    try {
                        List<String> words = idfMissWordService.getRecords(handle_size);
                        System.out.println("====== miss word size : " + words.size());
                        if(words.size() > 0) {
                            Map<String, Integer> map = Maps.newHashMap();
                            for(String word : words) {
                                if(map.containsKey(word)) {
                                    int num = map.get(word) + 1;
                                    map.put(word, num);
                                } else {
                                    map.put(word, 1);
                                }
                            }
                            List<IDFMissWord> idfMissWords = Lists.newArrayList();
                            for(Map.Entry<String, Integer> entry : map.entrySet()) {
                                IDFMissWord idfMissWord = new IDFMissWord();
                                idfMissWord.setWord(entry.getKey());
                                idfMissWord.setNum(entry.getValue());
                                idfMissWords.add(idfMissWord);
                            }
                            idfMissWordService.handle(idfMissWords);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.MINUTES);
                }
            }
        }
    }

}
