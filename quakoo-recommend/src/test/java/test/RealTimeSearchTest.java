package test;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.bean.SearchRes;
import com.quakoo.framework.ext.recommend.service.ext.RealTimeSearchService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class RealTimeSearchTest {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");
        RealTimeSearchService realTimeSearchService = context.getBean(RealTimeSearchService.class);

        List<String> words = Lists.newArrayList();
        words.add("监察");
        words.add("质量");
        words.add("炼钢厂");
        realTimeSearchService.search(words);

        long startTime = System.currentTimeMillis();
        List<SearchRes> list = realTimeSearchService.search(words);
//        for(SearchRes one : list) {
//            System.out.println(one.getId() + " " + one.getScore() + " " + one.getTime());
//        }
        System.out.println("time : " + (System.currentTimeMillis() - startTime) + ", size : " + list.size());


        System.exit(1);
    }

}
