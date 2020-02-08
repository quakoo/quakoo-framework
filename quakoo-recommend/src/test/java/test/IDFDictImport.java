package test;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.reflect.ClassloadUtil;
import com.quakoo.framework.ext.recommend.model.IDFDict;
import com.quakoo.framework.ext.recommend.service.IDFDictService;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class IDFDictImport {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    private static List<IDFDict> readTxt() throws Exception {
        List<IDFDict> res = Lists.newArrayList();
        InputStream is = ClassloadUtil.getClassLoader().getResourceAsStream("idf_dict.txt");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line=null;
            while((line=br.readLine())!=null) {
                String[] kv=line.trim().split(" ");
                IDFDict idfDict = new IDFDict();
                idfDict.setWord(kv[0]);
                idfDict.setWeight(Double.parseDouble(kv[1]));
                res.add(idfDict);
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(br);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");

        IDFDictService idfDictService = context.getBean(IDFDictService.class);

        Map<String, Double> map = idfDictService.loadIDFMap();

        System.out.println(map.size());

//        List<IDFDict> list = readTxt();
//        List<List<IDFDict>> lists = Lists.partition(list, 3000);
//        for(List<IDFDict> one : lists) {
//            int res = idfDictService.insert(one);
//            System.out.println("insert num : " + res);
//        }


//        System.exit(1);
    }

}
