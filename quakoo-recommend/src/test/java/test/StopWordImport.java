package test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.reflect.ClassloadUtil;
import com.quakoo.framework.ext.recommend.model.StopWord;
import com.quakoo.framework.ext.recommend.service.StopWordService;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

public class StopWordImport {

    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            "recommend-service-config.xml");

    private static List<StopWord> readTxt() throws Exception {
        Set<String> set = Sets.newLinkedHashSet();
        InputStream is = ClassloadUtil.getClassLoader().getResourceAsStream("stop_words.txt");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line=null;
            while((line=br.readLine())!=null) {
                set.add(line);
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(br);
        }
        List<StopWord> res = Lists.newArrayList();
        for(String one : set) {
            StopWord stopWord = new StopWord();
            stopWord.setWord(one);
            res.add(stopWord);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================");
        StopWordService stopWordService = context.getBean(StopWordService.class);

//        StopWord stopWord = new StopWord();
//        stopWord.setWord("上了");
//        System.out.println(stopWordService.insert(Lists.newArrayList(stopWord)));

//        Set<String> set = stopWordService.loadStopWords();
//        System.out.println("==== : "+set.size());
//        List<StopWord> list = readTxt();
//
//        int res = stopWordService.insert(list);
//        System.out.println(res);



    }

}
