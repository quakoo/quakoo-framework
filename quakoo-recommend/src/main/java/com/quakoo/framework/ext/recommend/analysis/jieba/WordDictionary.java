package com.quakoo.framework.ext.recommend.analysis.jieba;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.reflect.ClassloadUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;

public class WordDictionary {

    private Logger logger = LoggerFactory.getLogger(WordDictionary.class);

    private static WordDictionary singleton;

    private static final String PROB_EMIT = "prob_emit.txt";
    private static char[] states = new char[] { 'B', 'M', 'E', 'S' };
    private static Map<Character, Map<Character, Double>> emit;
    private static Map<Character, Double> start;
    private static Map<Character, Map<Character, Double>> trans;
    private static Map<Character, char[]> prevStatus;
    private static Double MIN_FLOAT = -3.14e100;

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
            singleton.init();
            singleton.initMain();
            singleton.initDict();
        }
        return singleton;
    }

    private void init() {
        long startTime = System.currentTimeMillis();
        prevStatus = Maps.newHashMap();
        prevStatus.put('B', new char[] { 'E', 'S' });
        prevStatus.put('M', new char[] { 'M', 'B' });
        prevStatus.put('S', new char[] { 'S', 'E' });
        prevStatus.put('E', new char[] { 'B', 'M' });

        start = Maps.newHashMap();
        start.put('B', -0.26268660809250016);
        start.put('E', -3.14e+100);
        start.put('M', -3.14e+100);
        start.put('S', -1.4652633398537678);

        trans = Maps.newHashMap();
        Map<Character, Double> transB = Maps.newHashMap();
        transB.put('E', -0.510825623765990);
        transB.put('M', -0.916290731874155);
        trans.put('B', transB);
        Map<Character, Double> transE = Maps.newHashMap();
        transE.put('B', -0.5897149736854513);
        transE.put('S', -0.8085250474669937);
        trans.put('E', transE);
        Map<Character, Double> transM = Maps.newHashMap();
        transM.put('E', -0.33344856811948514);
        transM.put('M', -1.2603623820268226);
        trans.put('M', transM);
        Map<Character, Double> transS = Maps.newHashMap();
        transS.put('B', -0.7211965654669841);
        transS.put('S', -0.6658631448798212);
        trans.put('S', transS);

        emit = Maps.newHashMap();
        InputStream is = ClassloadUtil.getClassLoader().getResourceAsStream(PROB_EMIT);
        if(is == null) throw new IllegalStateException("prob_emit.txt is null!");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            Map<Character, Double> values = null;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("\t");
                if (tokens.length == 1) {
                    values = Maps.newHashMap();
                    emit.put(tokens[0].charAt(0), values);
                }
                else {
                    values.put(tokens[0].charAt(0), Double.valueOf(tokens[1]));
                }
            }
            logger.info(String.format("WordDictionary init finished, time elapsed %d ms.", System.currentTimeMillis() - startTime));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(br);
        }
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
                            CompletionService<Map<String, Double>> completionService = new ExecutorCompletionService<>(exs);
                            for(InputStream inputStream : inputStreams) {
                                completionService.submit(new DictLoader(inputStream, total, _dict));
                            }
                            for(int i = 0; i < inputStreams.size(); i++){
                                Map<String, Double> map = completionService.take().get();
                                freqs.putAll(map);
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

    class DictLoader implements Callable<Map<String, Double>> {

        private InputStream is;
        private Double total;
        private DictSegment _dict;

        public DictLoader(InputStream is, Double total, DictSegment _dict) {
            this.is = is;
            this.total = total;
            this._dict = _dict;
        }

        @Override
        public Map<String, Double> call() throws Exception {
            Map<String, Double> res = Maps.newHashMap();
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
                    res.put(word, Math.log(freq / total));
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(br);
            }
            return res;
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

    public void cut(String sentence, List<String> tokens) {
        StringBuilder chinese = new StringBuilder();
        StringBuilder other = new StringBuilder();
        for (int i = 0; i < sentence.length(); ++i) {
            char ch = sentence.charAt(i);
            if (CharacterUtil.isChineseLetter(ch)) {
                if (other.length() > 0) {
                    processOtherUnknownWords(other.toString(), tokens);
                    other = new StringBuilder();
                }
                chinese.append(ch);
            }
            else {
                if (chinese.length() > 0) {
                    viterbi(chinese.toString(), tokens);
                    chinese = new StringBuilder();
                }
                other.append(ch);
            }

        }
        if (chinese.length() > 0)
            viterbi(chinese.toString(), tokens);
        else {
            processOtherUnknownWords(other.toString(), tokens);
        }
    }

    public void viterbi(String sentence, List<String> tokens) {
        Vector<Map<Character, Double>> v = new Vector<Map<Character, Double>>();
        Map<Character, Node> path = new HashMap<Character, Node>();

        v.add(new HashMap<Character, Double>());
        for (char state : states) {
            Double emP = emit.get(state).get(sentence.charAt(0));
            if (null == emP)
                emP = MIN_FLOAT;
            v.get(0).put(state, start.get(state) + emP);
            path.put(state, new Node(state, null));
        }

        for (int i = 1; i < sentence.length(); ++i) {
            Map<Character, Double> vv = new HashMap<Character, Double>();
            v.add(vv);
            Map<Character, Node> newPath = new HashMap<Character, Node>();
            for (char y : states) {
                Double emp = emit.get(y).get(sentence.charAt(i));
                if (emp == null)
                    emp = MIN_FLOAT;
                Pair<Character> candidate = null;
                for (char y0 : prevStatus.get(y)) {
                    Double tranp = trans.get(y0).get(y);
                    if (null == tranp)
                        tranp = MIN_FLOAT;
                    tranp += (emp + v.get(i - 1).get(y0));
                    if (null == candidate)
                        candidate = new Pair<Character>(y0, tranp);
                    else if (candidate.freq <= tranp) {
                        candidate.freq = tranp;
                        candidate.key = y0;
                    }
                }
                vv.put(y, candidate.freq);
                newPath.put(y, new Node(y, path.get(candidate.key)));
            }
            path = newPath;
        }
        double probE = v.get(sentence.length() - 1).get('E');
        double probS = v.get(sentence.length() - 1).get('S');
        Vector<Character> posList = new Vector<Character>(sentence.length());
        Node win;
        if (probE < probS)
            win = path.get('S');
        else
            win = path.get('E');

        while (win != null) {
            posList.add(win.value);
            win = win.parent;
        }
        Collections.reverse(posList);

        int begin = 0, next = 0;
        for (int i = 0; i < sentence.length(); ++i) {
            char pos = posList.get(i);
            if (pos == 'B')
                begin = i;
            else if (pos == 'E') {
                tokens.add(sentence.substring(begin, i + 1));
                next = i + 1;
            }
            else if (pos == 'S') {
                tokens.add(sentence.substring(i, i + 1));
                next = i + 1;
            }
        }
        if (next < sentence.length())
            tokens.add(sentence.substring(next));
    }

    private void processOtherUnknownWords(String other, List<String> tokens) {
        Matcher mat = CharacterUtil.reSkip.matcher(other);
        int offset = 0;
        while (mat.find()) {
            if (mat.start() > offset) {
                tokens.add(other.substring(offset, mat.start()));
            }
            tokens.add(mat.group());
            offset = mat.end();
        }
        if (offset < other.length())
            tokens.add(other.substring(offset));
    }

}
