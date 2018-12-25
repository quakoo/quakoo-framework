package com.quakoo.baseFramework.config.test;
import java.util.Map;

import com.quakoo.baseFramework.config.ConfigProperty;

/**
 * 
 * @author LiYongbiao
 *
 */
public class TestStatic {

	@ConfigProperty(path="testStaticmap",json=true)
	public static Map map;
	
}
