package com.quakoo.baseFramework.config.test;

import java.util.Map;

/**
 * 
 * @author LiYongbiao
 *
 */
import com.quakoo.baseFramework.config.ConfigProperty;


/**
 * 
 * @author LiYongbiao
 *
 */
public class TestObject {

	@ConfigProperty(path="TestObjectmap",json=true)
	public Map map;
	
	@ConfigProperty(path="TestObjectsss")
	public int sss;
}
