package com.quakoo.framework.ext.recommend.analysis.jieba;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.reflect.ClassloadUtil;
import com.quakoo.framework.ext.recommend.analysis.jieba.viterbi.FinalSeg;
import com.sun.tools.internal.ws.processor.util.DirectoryUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class WordDictionary {

    private Logger logger = LoggerFactory.getLogger(WordDictionary.class);

    private static WordDictionary singleton;
    private static final String MAIN_DICT = "dict.txt";

    private final Map<String, Double> freqs = Maps.newHashMap();
    private Double minFreq = Double.MAX_VALUE;
    private Double total = 0.0;
    private DictSegment _dict;

    private WordDictionary() {
    }

    public synchronized static WordDictionary getInstance() {
        if (null == singleton) {
            singleton = new WordDictionary();
            singleton.initMain();
            singleton.initDict();
        }
        return singleton;
    }

    public void initDict() {
        long startTime = System.currentTimeMillis();
        ExecutorService exs = null;
        try {
            URL url = ClassloadUtil.getClassLoader().getResource("dict");
            if(url != null) {
                File folder = FileUtils.getFile(url.getPath());
                if(folder.exists() && folder.isDirectory()) {
                    String[] fileNames = folder.list();
                    if(fileNames != null) {
                        List<InputStream> inputStreams = Lists.newArrayList();
                        for(String fileName : fileNames) {
                            if(fileName.contains(".dict")) {
                                InputStream is = ClassloadUtil.getClassLoader().getResourceAsStream("dict/" + fileName);
                                inputStreams.add(is);
                            }
                        }
                        if(inputStreams.size() > 0) {
                            exs = Executors.newFixedThreadPool(inputStreams.size());
                            CompletionService<Void> completionService = new ExecutorCompletionService<Void>(exs);
                            for(InputStream inputStream : inputStreams) {
                                completionService.submit(new DictLoader(inputStream));
                            }
                            for(int i = 0; i < inputStreams.size(); i++){
                                completionService.take().get();
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            if(exs != null) exs.shutdown();
        }
        logger.info(String.format("WordDictionary initDict finished, time elapsed %d ms.", System.currentTimeMillis() - startTime));
    }

    public void initMain() {
        long startTime = System.currentTimeMillis();
        _dict = new DictSegment((char) 0);
        InputStream is = ClassloadUtil.getClassLoader().getResourceAsStream(MAIN_DICT);
        if(is == null) throw new IllegalStateException("dict.txt is null!");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");
                if (tokens.length < 2)
                    continue;
                String word = tokens[0];
                double freq = Double.parseDouble(tokens[1]);
                total += freq;
                word = addWord(word);
                freqs.put(word, freq);
            }
            for (Map.Entry<String, Double> entry : freqs.entrySet()) {
                entry.setValue((Math.log(entry.getValue() / total)));
                minFreq = Math.min(entry.getValue(), minFreq);
            }
            logger.info(String.format("WordDictionary initMain finished, time elapsed %d ms.", System.currentTimeMillis() - startTime));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(br);
        }
    }

    private String addWord(String word) {
        if (null != word && !"".equals(word.trim())) {
            String key = word.trim().toLowerCase(Locale.getDefault());
            _dict.fillSegment(key.toCharArray());
            return key;
        }
        else
            return null;
    }

    class DictLoader implements Callable<Void> {
        private InputStream is;
        public DictLoader(InputStream is) {
            this.is = is;
        }
        @Override
        public Void call() throws Exception {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                while (br.ready()) {
                    String line = br.readLine();
                    String[] tokens = line.split("[\t ]+");
                    if (tokens.length < 1) {
                        continue;
                    }
                    String word = tokens[0];
                    double freq = 3.0d;
                    if (tokens.length == 2)
                        freq = Double.parseDouble(tokens[1]);
                    word = addWord(word);
                    freqs.put(word, Math.log(freq / total));
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(br);
            }
            return null;
        }
    }

    public DictSegment getTrie() {
        return this._dict;
    }

    public boolean containsWord(String word) {
        return freqs.containsKey(word);
    }


    public Double getFreq(String key) {
        if (containsWord(key))
            return freqs.get(key);
        else
            return minFreq;
    }

}
