package com.quakoo.framework.ext.chat.util;

import com.quakoo.baseFramework.reflect.ClassloadUtil;

import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {

	private Properties properties;
	
	private PropertyUtil(String fileName) {
		try {
			InputStream inputStream = ClassloadUtil.getClassLoader()
					.getResourceAsStream(fileName);
			if (inputStream != null) {
				Properties properties = new Properties();
				properties.load(inputStream);
				this.properties = properties;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static PropertyUtil getInstance(String fileName) {
		PropertyUtil propertyUtil = new PropertyUtil(fileName);
		return propertyUtil;
	}
	
	public String getProperty(String propertyName) {
		if(null != properties) {
			return properties.getProperty(propertyName);
		}
		return null;
	}
	
}
