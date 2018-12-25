package com.quakoo.baseFramework.config.test;
import java.util.Map;

import com.quakoo.baseFramework.config.ReloadInstance;
import com.quakoo.baseFramework.config.ConfigProperty;
import com.quakoo.baseFramework.config.ReloadInstance;

/**
 * 
 * @author LiYongbiao
 *
 */
public class TestReload implements ReloadInstance {

	@ConfigProperty(path="TestObjectReloadmap",json=true)
	public Map map;
	
	
	@Override
	public void reloadOnPropertyChange() {
		System.out.println("reload...............");
		
	}


	@Override
	public void init() {
		
	}

}
