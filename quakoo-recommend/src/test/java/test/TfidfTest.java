package test;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.bean.Keyword;
import com.quakoo.framework.ext.recommend.service.HotWordService;
import com.quakoo.framework.ext.recommend.service.TFIDFService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TfidfTest {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");
        TFIDFService tfidfService = context.getBean(TFIDFService.class);
        HotWordService hotWordService = context.getBean(HotWordService.class);

//        List<String> contents = Lists.newArrayList();
//        contents.add("防拐");
//        contents.add("防拐");
//        contents.add("幼儿园");
//        contents.add("孩子上了幼儿园 安全防拐教育要做好");
//        contents.add("2020年全球经济走势谨慎乐观 三大变数或起关键作用");
//        contents.add("中国五冶集团工程总承包公司组织召开2019年度领导干部述职述廉及民主评议大会");
//        int topN=5;
//        for(String content : contents) {
//            List<Keyword> keywords = tfidfService.analyze(content, topN);
//            for(Keyword word : keywords) {
//                System.out.println(word.getWord() + " : " + word.getTfidfWeight());
//            }
//            System.out.println("==================");
//        }
//
//        Thread.sleep(2000);
//
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(new Date());
//        calendar.add(Calendar.HOUR_OF_DAY, 1);
//
//        hotWordService.handle(calendar.getTime());
//
//        List<String> hotwords = hotWordService.getLastHotWords();
//        System.out.println(hotwords.toString());

//        System.exit(1);
    }

}
