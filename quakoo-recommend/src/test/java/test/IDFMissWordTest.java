package test;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.model.IDFMissWord;
import com.quakoo.framework.ext.recommend.service.IDFMissWordService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;


public class IDFMissWordTest {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");
        IDFMissWordService idfMissWordService = context.getBean(IDFMissWordService.class);

//        List<IDFMissWord> list = Lists.newArrayList();
//
//        IDFMissWord one = new IDFMissWord();
//        one.setWord("aa");
//        one.setNum(1);
//        list.add(one);
//
//        IDFMissWord two = new IDFMissWord();
//        two.setWord("bb");
//        two.setNum(1);
//        list.add(two);
//
//        IDFMissWord three = new IDFMissWord();
//        three.setWord("cc");
//        three.setNum(1);
//        list.add(three);
//
//        idfMissWordService.handle(list);
//
//        System.exit(1);
    }

}
