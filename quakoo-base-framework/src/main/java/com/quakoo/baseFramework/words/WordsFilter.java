package com.quakoo.baseFramework.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * @author lichao 过滤敏感词
 */
public class WordsFilter {
	private Logger log = LoggerFactory.getLogger(WordsFilter.class);
	private BloomFilter<CharSequence> filter;

	public WordsFilter(String fileName) throws IOException {
		this(fileName, 0.03);
	}

	public WordsFilter(String fileName, double fpp) throws IOException {
		String classpath = WordsFilter.class.getClassLoader().getResource("").getPath();
		File file = new File(classpath+"/"+fileName);
		if (!file.exists())
			throw new FileNotFoundException(fileName);

		Set<String> insertions = Files.readLines(file, Charsets.UTF_8, new LineProcessor<Set<String>>() {
			final Set<String> result = Sets.newHashSet();

			@Override
			public boolean processLine(String line) {
				result.add(line);
				return true;
			}

			@Override
			public Set<String> getResult() {
				return result;
			}
		});

		init(insertions, fpp);

	}

	public WordsFilter(Set<String> insertions) {
		this(insertions, 0.03);
	}

	public WordsFilter(Set<String> insertions, double fpp) {
		init(insertions, fpp);
	}

	private void init(Set<String> insertions, double fpp) {
		if (insertions == null || insertions.size() == 0)
			throw new IllegalArgumentException("the size of insertions must be > 0 !");

		filter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), insertions.size(), fpp);

		for (String insertion : insertions) {
			filter.put(insertion);
		}
		log.info("load sensitive word:" + insertions.size());
	}

	public boolean mightContain(String object) {
		return filter.mightContain(object);
	}

	public static void main(String[] args) throws IOException {
		WordsFilter filter = new WordsFilter("sensitiveword.txt");
		System.out.println(filter.mightContain("sex"));
	}
}
