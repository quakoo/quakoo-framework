package test;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.bean.ESField;
import com.quakoo.framework.ext.recommend.model.SyncInfo;
import com.quakoo.framework.ext.recommend.service.SyncInfoService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class SyncInfoTest {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");

        SyncInfoService syncInfoService = context.getBean(SyncInfoService.class);

//        boolean del = syncInfoService.delete(2);
//        System.out.println(del);

//        boolean sign = syncInfoService.updateTrackingValue(2, 2);
//        System.out.println(sign);

//        List<SyncInfo> syncInfos = syncInfoService.getSyncInfos();
//        for(SyncInfo one : syncInfos) {
//            System.out.println(one.toString());
//        }

//        List<ESField> list = Lists.newArrayList();
//        ESField a = new ESField("id", "true", "long", null, "id");
//        ESField b = new ESField("title", "true", "text", "jieba_index", "title");
//        ESField c = new ESField("content","true", "text", "jieba_index", "content");
//        ESField d = new ESField("type", "true", "integer", null, "type");
//        ESField e = new ESField("lastUpdateTime", "true", "long", null, "lastUpdateTime");
//        list.add(a);
//        list.add(b);
//        list.add(c);
//        list.add(d);
//        list.add(e);
//
//        SyncInfo syncInfo = new SyncInfo();
//        syncInfo.setSql("select * from article where status = 2");
//        syncInfo.setEsIndex("article");
//        syncInfo.setEsFields(list);
//        syncInfo.setEsId("id");
//        syncInfo.setTrackingColumn("lastUpdateTime");
//        syncInfo.setBatchSize(500);
//
//        boolean res = syncInfoService.insert(syncInfo);
//        System.out.println(res);

//        System.exit(1);
    }

}
