package com.quakoo.baseFramework.words;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.paoding.analysis.analyzer.PaodingAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.google.common.collect.Sets;

/**
 * @author lichao
 * 
 */
public class Paoding {

    private static Analyzer analyzer = new PaodingAnalyzer();

    public static Set<String> analyze(String text) throws IOException {
        return analyze(text, 0, new ArrayList<String>());
    }

    public static Set<String> analyze(String text, int minSize, List<String> filter) throws IOException {

        Set<String> words = Sets.newLinkedHashSet();
        TokenStream stream = null;
        try {
            stream = analyzer.tokenStream("text", text);
            stream.reset();
            CharTermAttribute charTerm = stream.addAttribute(CharTermAttribute.class);
            while (stream.incrementToken()) {
                String word = charTerm.toString();
                if (word.length() >= minSize && !filter.contains(word)) {
                    words.add(word);
                }
            }
        } finally {
            if (stream != null)
                stream.close();
        }

        return words;
    }
}
