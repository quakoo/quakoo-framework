package com.quakoo.baseFramework.config.test;

import com.quakoo.baseFramework.config.ConfigServer;
import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.config.ZkResult;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * 
 * @author LiYongbiao
 *
 */
public class TestConfig {

	static String zkAddress="10.0.5.108:2181,10.0.5.109:2181,10.0.5.110:2181";
	static String root="/testroot";
	static String projectName="testProject";
	
	public static void main(String[] sdf) throws Exception{
		//listen();
		setValue("/testroot/testStaticmap","{\"kkkk\":\"greg\"}","");
		setValue("/testroot/TestObjectmap","{\"f0gh\":\"gerg\"}",null);
		setValue("/testroot/TestObjectsss","12312",null);
		setValue("/testroot/TestObjectReloadmap","{\"s0df\":\"dgsdgw\"}","127.0.1");
		
	}
	public static void listen() throws Exception{

		final TestObject testObject=new TestObject();
		TestReload testReloadre=new TestReload();
		ConfigServer configServer=new ConfigServer(zkAddress, root, projectName);
		configServer.initStaticClass(TestStatic.class);
		configServer.initObject(testObject);
		configServer.initObject(testReloadre);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(;;){
					System.out.println(TestStatic.map);
					System.out.println(""+testObject.sss+"_"+testObject.map);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		Thread.sleep(10000000);
	}
	
	
	public static void setValue(String path,String value,String ip) throws Exception{
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
		CuratorFramework client = builder.connectString(zkAddress)
				.sessionTimeoutMs(10000)
				.connectionTimeoutMs(5000)
				.canBeReadOnly(false)
				.retryPolicy(new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
				.namespace(projectName)
				.defaultData(null)
				.build();
		client.start();
		
		ZkResult zkResult=new ZkResult();
		zkResult.setResult(value);
		if(ip!=null){
			zkResult.getNotAllowIps().add(ip);
		}
	
		Stat stat=client.checkExists().forPath(path);
		if(stat==null)
		client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path,null );
		client.setData().forPath(path, JsonUtils.objectMapper.writeValueAsBytes(zkResult) );
	}
	
	
	
}
