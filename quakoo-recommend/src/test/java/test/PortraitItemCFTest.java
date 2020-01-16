package test;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import com.quakoo.framework.ext.recommend.context.handle.PortraitItemCFSchedulerContextHandle;
import com.quakoo.framework.ext.recommend.dao.PortraitItemCFDao;
import com.quakoo.framework.ext.recommend.model.PortraitItemCF;
import com.quakoo.framework.ext.recommend.service.PortraitItemCFService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class PortraitItemCFTest {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");
        PortraitItemCFService portraitItemCFService = context.getBean(PortraitItemCFService.class);

        portraitItemCFService.record(1, "质量监察的“你们”");
        portraitItemCFService.record(1, "班组标准化建设 炼钢厂安全工作这样落实");
        portraitItemCFService.record(1, "关于印发《临汾市2019年钢铁、焦化行业深度减排实施方案》的通知");



//        PortraitItemCF a = new PortraitItemCF();
//        a.setUid(1);
//        List<PortraitWord> items = Lists.newArrayList();
//        PortraitWord aword = new PortraitWord();
//        aword.setWord("a");
//        aword.setWeight(0.01);
//        items.add(aword);
//        a.setWords(items);
//
//        PortraitItemCF b = new PortraitItemCF();
//        b.setUid(2);
//        List<PortraitWord> items2 = Lists.newArrayList();
//        PortraitWord bword = new PortraitWord();
//        bword.setWord("b");
//        bword.setWeight(0.02);
//        items2.add(bword);
//        b.setWords(items2);
//
//        int res = portraitItemCFDao.insert(Lists.newArrayList(a, b));
//        System.out.println(res);



//        List<PortraitItemCF> list = portraitItemCFDao.load(Lists.newArrayList(1l, 2l));
//        System.out.println(list.toString());

//        for(PortraitItemCF one : list) {
//            List<PortraitWord> items = Lists.newArrayList();
//            PortraitWord word = new PortraitWord();
//            word.setWord("aaaa");
//            word.setWeight(0.8);
//            items.add(word);
//            one.setWords(items);
//        }
//        int res = portraitItemCFDao.update(list);
//        System.out.println(res);

//        System.exit(1);
    }

}
