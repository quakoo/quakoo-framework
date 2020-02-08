package test;

import com.quakoo.framework.ext.recommend.service.RecommendService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RecommendTest {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");
        RecommendService recommendService = context.getBean(RecommendService.class);
        recommendService.recommend(1l);

        System.exit(1);
    }

}
