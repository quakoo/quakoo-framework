package com.quakoo.baseFramework.config;


/**
 * 
 * @author LiYongbiao
 *
 */
public interface ReloadInstance {

	/**
	 * 有错误请抛出来<br>
	 * 程序结构：<br>
	 * 1.加载zk数据并转换<br>
	 * 2.赋值到temp<br>
	 * 3.批量替换原有的属性<br>
	 * <br>
	 * 不要首先就修改原有属性，否则一旦zk数据不规范抛出错误后，会影响现有系统。<br>
	 * 
	 */
	public void reloadOnPropertyChange() throws Exception;
	
	/**
	 * 
	 * @throws Exception 错误抛出。系统初始化失败。
	 */
	public void init() throws Exception;
}
