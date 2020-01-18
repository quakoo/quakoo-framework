package com.quakoo.framework.ext.recommend.analysis.jieba;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiebaSegmenter {

    private Logger logger = LoggerFactory.getLogger(JiebaSegmenter.class);

    private static JiebaSegmenter singleton;
    private static WordDictionary wordDict;

    private JiebaSegmenter() {
    }

    public synchronized static JiebaSegmenter getInstance() {
        if (null == singleton) {
            singleton = new JiebaSegmenter();
            wordDict = WordDictionary.getInstance();
        }
        return singleton;
    }

    private Map<Integer, List<Integer>> createDAG(String sentence) {
        Map<Integer, List<Integer>> dag = Maps.newHashMap();
        DictSegment trie = wordDict.getTrie();
        char[] chars = sentence.toCharArray();
        int N = chars.length;
        int i = 0, j = 0;
        while (i < N) {
            Hit hit = trie.match(chars, i, j - i + 1);
            if (hit.isPrefix() || hit.isMatch()) {
                if (hit.isMatch()) {
                    if (!dag.containsKey(i)) {
                        List<Integer> value = Lists.newArrayList();
                        dag.put(i, value);
                        value.add(j);
                    } else
                        dag.get(i).add(j);
                }
                j += 1;
                if (j >= N) {
                    i += 1;
                    j = i;
                }
            } else {
                i += 1;
                j = i;
            }
        }
        for (i = 0; i < N; ++i) {
            if (!dag.containsKey(i)) {
                List<Integer> value = Lists.newArrayList();
                value.add(i);
                dag.put(i, value);
            }
        }
        return dag;
    }

    private Map<Integer, Pair<Integer>> calc(String sentence, Map<Integer, List<Integer>> dag) {
        int N = sentence.length();
        HashMap<Integer, Pair<Integer>> route = Maps.newHashMap();
        route.put(N, new Pair<Integer>(0, 0.0));
        for (int i = N - 1; i > -1; i--) {
            Pair<Integer> candidate = null;
            for (Integer x : dag.get(i)) {
                double freq = wordDict.getFreq(sentence.substring(i, x + 1)) + route.get(x + 1).freq;
                if (null == candidate) {
                    candidate = new Pair<Integer>(x, freq);
                } else if (candidate.freq < freq) {
                    candidate.freq = freq;
                    candidate.key = x;
                }
            }
            route.put(i, candidate);
        }
        return route;
    }

    public List<String> sentenceProcess(String sentence) {
        List<String> tokens = Lists.newArrayList();
        int N = sentence.length();
        Map<Integer, List<Integer>> dag = createDAG(sentence);
        Map<Integer, Pair<Integer>> route = calc(sentence, dag);

        int x = 0;
        int y = 0;
        String buf;
        StringBuilder sb = new StringBuilder();
        while (x < N) {
            y = route.get(x).key + 1;
            String lWord = sentence.substring(x, y);
            if (y - x == 1)
                sb.append(lWord);
            else {
                if (sb.length() > 0) {
                    buf = sb.toString();
                    sb = new StringBuilder();
                    if (buf.length() == 1) {
                        tokens.add(buf);
                    } else {
                        if (wordDict.containsWord(buf)) {
                            tokens.add(buf);
                        } else {
                            wordDict.cut(buf, tokens);
                        }
                    }
                }
                tokens.add(lWord);
            }
            x = y;
        }
        buf = sb.toString();
        if (buf.length() > 0) {
            if (buf.length() == 1) {
                tokens.add(buf);
            } else {
                if (wordDict.containsWord(buf)) {
                    tokens.add(buf);
                } else {
                    wordDict.cut(buf, tokens);
                }
            }

        }
        return tokens;
    }

    public static void main(String[] args) {
        JiebaSegmenter jiebaSegmenter = JiebaSegmenter.getInstance();
        List<String> list = jiebaSegmenter.sentenceProcess("150");
        System.out.println(list.toString());
    }

}
