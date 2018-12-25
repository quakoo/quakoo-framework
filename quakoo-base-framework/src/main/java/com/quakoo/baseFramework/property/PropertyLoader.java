package com.quakoo.baseFramework.property;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.quakoo.baseFramework.reflect.ClassloadUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 动态配置文件加载工具。配置文件修改后，可以动态获取到修改后的值。
 * 
 * @author liyongbiao
 * 
 */
public class PropertyLoader {
	private static final Logger logger = LoggerFactory
			.getLogger(PropertyLoader.class);

	private static Map<String, PropertyLoader> systemPropertieMap = new HashMap<String, PropertyLoader>();

	private Properties properties;

	private String fileName;

	private long lastFreshTime;

	static final int refreshDistanceInSecond = 60000;

	private static boolean closeThread = false;

	public Properties getProperties() {
		return properties;
	}

	static {
		newThreadLoadProperties();
	}

	public static void closeThread() {
		closeThread = true;
	}

	public static PropertyLoader getInstance(String fileName) {
		if (!systemPropertieMap.containsKey(fileName)) {
			systemPropertieMap.put(fileName, init(fileName));
		}
		return systemPropertieMap.get(fileName);
	}

	static synchronized PropertyLoader init(String fileName) {
		PropertyLoader systemProperties = new PropertyLoader(fileName);
		PropertyLoader.logger
				.info("init() - systemProperties=" + systemProperties + ", refreshDistanceInSecond=" + PropertyLoader.refreshDistanceInSecond / 1000); //$NON-NLS-1$ //$NON-NLS-2$
		return systemProperties;
	}

	private PropertyLoader(String fileName) {
		this.fileName = fileName;
		InputStream inputStream = ClassloadUtil.getClassLoader()
				.getResourceAsStream(fileName);
		if (inputStream == null) {
			throw new RuntimeException("can not read  file : " + fileName);
		}
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			this.properties = properties;
			this.lastFreshTime = System.currentTimeMillis();
			this.fileName = fileName;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void newThreadLoadProperties() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					try {
						Thread.sleep(refreshDistanceInSecond);
						if (closeThread) {
							break;
						}
						for (String key : systemPropertieMap.keySet()) {
							String fileName = key;
							PropertyLoader systemProperties = systemPropertieMap
									.get(key);
							logger.debug("{} reloaded with the expiration:{}s",
									fileName, new Date());
							final URL url = ClassloadUtil.getClassLoader()
									.getResource(fileName);
							final Properties p = new Properties();
							InputStream in=null;
							try {
								File resultFile = new File(url.toURI());
								in = new FileInputStream(resultFile);
								p.load(in);
								systemProperties.properties = p;
							} catch (Exception e) {
								if (url.getPath().contains(".jar")) {
									return;
								} else {
									logger.warn(fileName + " load failed", e);
								}
								throw new IllegalStateException(e.getMessage());
							}finally {
								try {
									if(in!=null) {
										in.close();
									}
								} catch (Exception e) {
								}
							}

							systemProperties.lastFreshTime = System
									.currentTimeMillis();
						}

					} catch (Exception e) {
						logger.error("thread LoadProperties error", e);
					}
				}
			}
		}).start();
	}

	/**
	 * 
	 * 获取String类型的值，defaultValue为默认值，
	 * 当发现没有结果会优先返回默认值，如果没有默认值会返回null。
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String propertyName, String... defaultValue) {
		if (properties.getProperty(propertyName) != null) {
			return properties.getProperty(propertyName).trim();
		} else {
			if (defaultValue != null && defaultValue.length > 0) {
				return defaultValue[0];
			} else {
				return null;
			}
		}
	}

	
	/**
	 * 
	 * 获取boolean类型的值，defaultValue为默认值，
	 * 当发现没有结果或者结果转换错误会优先返回默认值，如果没有默认值会抛出异常。
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public boolean getBooleanProperty(String propertyName,
			boolean... defaultValue) {
		String value=null;
		try {
			 value = this.getProperty(propertyName);
			return Boolean.valueOf(value);
		} catch (Exception e) {
			if(value!=null){
				logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
			}
			if (defaultValue != null && defaultValue.length > 0) {
				return defaultValue[0];
			} else {
				throw new RuntimeException(this.fileName + " [" + propertyName
						+ "]参数转换错误", e);
			}
		}

	}

	public float getFloatProperty(String propertyName,
									  float... defaultValue) {
		String value=null;
		try {
			 value = this.getProperty(propertyName);
			return Float.valueOf(value);
		} catch (Exception e) {
			if(value!=null){
				logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
			}
			if (defaultValue != null && defaultValue.length > 0) {
				return defaultValue[0];
			} else {
				throw new RuntimeException(this.fileName + " [" + propertyName
						+ "]参数转换错误", e);
			}
		}

	}

	/**
	 * 
	 * 获取int类型的值，defaultValue为默认值，
	 * 当发现没有结果或者结果转换错误会优先返回默认值，如果没有默认值会抛出异常。
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public int getIntProperty(String propertyName, int... defaultValue) {
		int x = 0;
		String value = null;
		try {
			value = this.getProperty(propertyName);
			x = Integer.parseInt(value);
		} catch (Exception e) {
			if(value!=null){
				logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
			}
			if (defaultValue != null && defaultValue.length > 0) {
				return defaultValue[0];
			} else {
				throw new RuntimeException(this.fileName + " [" + propertyName
						+ "]参数转换错误", e);
			}
		}
		return x;
	}

	/**
	 * 获取long类型的值，defaultValue为默认值，
	 * 当发现没有结果或者结果转换错误会优先返回默认值，如果没有默认值会抛出异常。
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public long getLongProperty(String propertyName, long... defaultValue) {
		long x = 0;
		String value=null;
		try {
			 value = this.getProperty(propertyName);
			x = Long.parseLong(value);
		} catch (Exception e) {
			if(value!=null){
				logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
			}
			if (defaultValue != null && defaultValue.length > 0) {
				return defaultValue[0];
			} else {
				throw new RuntimeException(this.fileName + " [" + propertyName
						+ "]参数转换错误", e);
			}
		}
		return x;
	}

	public Map<String, String> getPropertiesParams(String param) {
		Map<String, String> paramsMap = new HashMap<String, String>();
		if (StringUtils.isNotBlank(param)) {
			String[] subs = param.split(";");
			for (String sub : subs) {
				if (StringUtils.isNotBlank(sub) && sub.contains(":")) {
					sub = sub.trim();
					String[] params = sub.split(":");
					paramsMap.put(params[0].trim(), params[1].trim());
				}
			}
		}
		return paramsMap;
	}

}
