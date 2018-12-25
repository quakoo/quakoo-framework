package com.quakoo.baseFramework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class TextFileUtils {

	public static List<String> readTxtFile(String filePath, String encoding)
			throws Exception {
		List<String> result = new ArrayList<>();
		if (StringUtils.isBlank(encoding)) {
			encoding = "utf-8";
		}
		File file = new File(filePath);
		if (file.isFile() && file.exists()) { // 判断文件是否存在
			InputStreamReader read = new InputStreamReader(new FileInputStream(
					file), encoding);// 考虑到编码格式
			try {
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				while ((lineTxt = bufferedReader.readLine()) != null) {
					result.add(lineTxt);
				}
			} finally {
				read.close();

			}

		} else {
			throw new RuntimeException("file not found");
		}

		return result;

	}

	public static void main(String argv[]) throws Exception {
		String filePath = "/Users/liyongbiao/Desktop/task.log";
		System.out.println(readTxtFile(filePath, "").size());
	}

}
