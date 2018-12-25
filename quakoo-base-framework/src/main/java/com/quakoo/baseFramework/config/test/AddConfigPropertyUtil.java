package com.quakoo.baseFramework.config.test;


import com.quakoo.baseFramework.config.ConfigServer;
import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.config.ZkResult;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author LiYongbiao
 *
 */
public class AddConfigPropertyUtil {

	//static String zkAddress="172.28.5.51:2181";
	static String zkAddress="172.28.6.140:2181,172.28.6.141:2181,172.28.6.142:2181";

	static String root="bufferCacheRoot";
	static String projectName="bufferCache";

	public static void main(String[] sdf) throws Exception{
		//online
		zkAddress="172.20.1.136:2181,172.20.1.137:2181,172.20.1.138:2181";

		//set value for bufferCache
		projectName="bufferCache";
		Map<String,List<String>> groupIps=new HashMap<>();
		List<String> ips1=new ArrayList<>();
		List<String> ips2=new ArrayList<>();
		List<String> ips3=new ArrayList<>();
		ips1.add("172.20.1.136:33001");
		ips1.add("172.20.1.137:33001");
		ips2.add("172.20.1.138:33001");
		ips2.add("172.20.1.154:33001");
		ips3.add("172.20.1.155:33001");
		groupIps.put("bufferCacheGroup1",ips1);
		groupIps.put("bufferCacheGroup2",ips2);
		groupIps.put("bufferCacheGroup3",ips3);
		setValue("/bufferCacheRoot/groupIps", JsonUtils.format(groupIps), null,zkAddress,"bufferCache");
		setValue("/bufferCacheRoot/oldGroupIps",JsonUtils.format(groupIps),null,zkAddress,"bufferCache");


				//set value for chunk
				projectName="chunk";
				Map<String,List<String>> chunkGroupIps=new HashMap<>();
				List<String> chunkIps1=new ArrayList<>();
				List<String> chunkIps2=new ArrayList<>();
				chunkIps1.add("172.20.1.136:32001");
				chunkIps1.add("172.20.1.137:32001");
				chunkIps2.add("172.20.1.138:32001");
				chunkIps2.add("172.20.1.154:32001");
				chunkGroupIps.put("chunkGroup1",chunkIps1);
				chunkGroupIps.put("chunkGroup2",chunkIps2);
				setValue("/chunkRoot/groupIps", JsonUtils.format(chunkGroupIps), null,zkAddress,"chunk");
				setValue("/chunkRoot/oldGroupIps",JsonUtils.format(chunkGroupIps),null,zkAddress,"chunk");


				//set value for computing 	computingNodes
				//@ConfigProperty(path = "computingNodes", json = true)
				//public  Map<String,Integer> computingNodes =new HashMap<>();
				projectName="computing";
				Map<String,Integer> computingNodes =new HashMap<>();
				computingNodes.put("172.20.1.136:34001",10);
				computingNodes.put("172.20.1.137:34001",10);
				computingNodes.put("172.20.1.138:34001",10);
				computingNodes.put("172.20.1.154:34001",10);
				computingNodes.put("172.20.1.155:34001",10);
				setValue("/computingRoot/computingNodes", JsonUtils.format(computingNodes), null,zkAddress,"computing");



				//set value for master
				projectName="master";
				setValue("/masterRoot/master_cleanStartTime","2",null,zkAddress,"master");
				setValue("/masterRoot/master_cleanEndTime","8",null,zkAddress,"master");



				//set value for master dispatchCenter_app2idc
				projectName="master";
				Map<Integer, Map<Object, Integer>> app2idc=new HashMap<>();
				Map<Object, Integer> map= new HashMap<Object, Integer>();
				map.put("1",10);
				map.put("2",0);
				app2idc.put(1001,map);
				//APPID为1001的 对应两个机房1，和2. 其中1的权重是20% 2的权重是80%。
				setValue("/masterRoot/dispatchCenter_app2idc", JsonUtils.format(app2idc), null,zkAddress,"master");

				//set value for master dispatchCenter_fstype2domain
				//@ConfigProperty(path = "dispatchCenter_fstype2domain", json = true,typeReference=MapIntegerStringTypeMode.class)
				//public Map<Integer,String> fstype2domain;
				projectName="master";
				Map<Integer,String> fstype2domain=new HashMap<>();
				fstype2domain.put(1,"http://d1.scloud.systoon.com");
				
//				fstype2domain.put(1,"http://172.28.6.136");
//				fstype2domain.put(2,"http://172.28.6.136");
				//fstype为1的IP是http://127.0.0.1:8880
				//fstype为2的IP是http://127.0.0.1:8881
				setValue("/masterRoot/dispatchCenter_fstype2domain", JsonUtils.format(fstype2domain), null,zkAddress,"master");


				//set value for master dispatchCenter_idc2domain
				//@ConfigProperty(path = "dispatchCenter_idc2domain", json = true,typeReference=MapIntegerStringTypeMode.class)
				//Map<Integer,String> idc2domain;
				projectName="master";
				Map<Integer,String> idc2domain=new HashMap<>();

				idc2domain.put(1,"http://d1.scloud.systoon.com");
				
//				idc为1的IP是http://127.0.0.1:8880
//				idc为2的IP是http://127.0.0.1:8881
				setValue("/masterRoot/dispatchCenter_idc2domain", JsonUtils.format(idc2domain), null,zkAddress,"master");





				//set value for master 	appInfoService_apps
				// Map<Integer, AppInfo> appinfoMap;
				projectName="master";
				Map<Integer, Map> appinfoMap=new HashMap<>();
				Map<String, Object> subMap= new HashMap<String, Object>();
				subMap.put("appid",1001);
				subMap.put("accessKeyId","sdfeggs");
				subMap.put("accessKeySecret","gregergher");
				subMap.put("callbakcUrl","http://127.0.0.1:8888");
				subMap.put("domain","http://127.0.0.1:8888");
				appinfoMap.put(1001,subMap);
				setValue("/masterRoot/appInfoService_apps", JsonUtils.format(appinfoMap), null,zkAddress,"master");

				projectName="master";
				List<String> secretService_keys =new ArrayList<>();
				secretService_keys.add("syswin#onlineSecretServiceKey");
				secretService_keys.add("syswin#onlineSecretServiceKey");
				setValue("/masterRoot/secretService_keys", JsonUtils.format(secretService_keys), null,zkAddress,"master");
		
		

		Thread.sleep(1000);

	}
	
	
	public static void main2(String[] sdf) throws Exception{
		//listen();test
		zkAddress="172.28.6.131:2181,172.28.6.132:2181,172.28.6.133:2181";

		//set value for bufferCache
//		root="bufferCacheRoot";
//		projectName="bufferCache";
//		Map<String,List<String>> groupIps=new HashMap<>();
//		List<String> ips1=new ArrayList<>();
//		List<String> ips2=new ArrayList<>();
//		List<String> ips3=new ArrayList<>();
//		ips1.add("172.28.6.131:33001");
//		ips1.add("172.28.6.132:33001");
//		ips2.add("172.28.6.133:33001");
//		ips3.add("172.28.6.134:33001");
//		groupIps.put("bufferCacheGroup1",ips1);
//		groupIps.put("bufferCacheGroup2",ips2);
//		groupIps.put("bufferCacheGroup3",ips3);
//		setValue("/bufferCacheRoot/groupIps", JsonUtils.format(groupIps), null);
//		setValue("/bufferCacheRoot/oldGroupIps",JsonUtils.format(groupIps),null);


				//set value for chunk
//				root="chunkRoot";
//				projectName="chunk";
//				Map<String,List<String>> groupIps=new HashMap<>();
//				List<String> ips1=new ArrayList<>();
//				List<String> ips2=new ArrayList<>();
//				ips1.add("172.28.6.131:32001");
//				ips1.add("172.28.6.132:32001");
//				ips2.add("172.28.6.133:32001");
//				ips2.add("172.28.6.134:32001");
//				groupIps.put("chunkGroup1",ips1);
//				groupIps.put("chunkGroup2",ips2);
//				setValue("/chunkRoot/groupIps", JsonUtils.format(groupIps), null);
//				setValue("/chunkRoot/oldGroupIps",JsonUtils.format(groupIps),null);


				//set value for computing 	computingNodes
				//@ConfigProperty(path = "computingNodes", json = true)
				//public  Map<String,Integer> computingNodes =new HashMap<>();
//				root="computingRoot";
//				projectName="computing";
//				Map<String,Integer> computingNodes =new HashMap<>();
//				computingNodes.put("172.28.6.131:34001",10);
//				computingNodes.put("172.28.6.132:34001",10);
//				computingNodes.put("172.28.6.133:34001",1);
//				computingNodes.put("172.28.6.134:34001",1);
//				setValue("/computingRoot/computingNodes", JsonUtils.format(computingNodes), null);



				//set value for master
//				root="masterRoot";
//				projectName="master";
//				setValue("/masterRoot/master_cleanStartTime","0",null);
//				setValue("/masterRoot/master_cleanEndTime","24",null);



				//set value for master dispatchCenter_app2idc
//				root="masterRoot";
//				projectName="master";
//				Map<Integer, Map<Object, Integer>> app2idc=new HashMap<>();
//				Map<Object, Integer> map= new HashMap<Object, Integer>();
//				map.put("1",2);
//				map.put("2",0);
//				app2idc.put(1001,map);
//				//APPID为1001的 对应两个机房1，和2. 其中1的权重是20% 2的权重是80%。
//				setValue("/masterRoot/dispatchCenter_app2idc", JsonUtils.format(app2idc), null);

				//set value for master dispatchCenter_fstype2domain
				//@ConfigProperty(path = "dispatchCenter_fstype2domain", json = true,typeReference=MapIntegerStringTypeMode.class)
				//public Map<Integer,String> fstype2domain;
//				root="masterRoot";
//				projectName="master";
//				Map<Integer,String> fstype2domain=new HashMap<>();
//				fstype2domain.put(1,"http://d1.scloud.systoon.com");
//				fstype2domain.put(2,"http://d1.scloud.systoon.com");
////				fstype2domain.put(1,"http://172.28.6.136");
////				fstype2domain.put(2,"http://172.28.6.136");
//				//fstype为1的IP是http://127.0.0.1:8880
//				//fstype为2的IP是http://127.0.0.1:8881
//				setValue("/masterRoot/dispatchCenter_fstype2domain", JsonUtils.format(fstype2domain), null);


				//set value for master dispatchCenter_idc2domain
				//@ConfigProperty(path = "dispatchCenter_idc2domain", json = true,typeReference=MapIntegerStringTypeMode.class)
				//Map<Integer,String> idc2domain;
//				root="masterRoot";
//				projectName="master";
//				Map<Integer,String> idc2domain=new HashMap<>();
//
//				idc2domain.put(1,"http://d1.scloud.systoon.com");
//				idc2domain.put(2,"http://d1.scloud.systoon.com");
////				idc为1的IP是http://127.0.0.1:8880
////				idc为2的IP是http://127.0.0.1:8881
//				setValue("/masterRoot/dispatchCenter_idc2domain", JsonUtils.format(idc2domain), null);





				//set value for master 	appInfoService_apps
				// Map<Integer, AppInfo> appinfoMap;
//				root="masterRoot";
//				projectName="master";
//				Map<Integer, Map> appinfoMap=new HashMap<>();
//				Map<String, Object> subMap= new HashMap<String, Object>();
//				subMap.put("appid",1001);
//				subMap.put("accessKeyId","sdfeggs");
//				subMap.put("accessKeySecret","gregergher");
//				subMap.put("callbakcUrl","http://127.0.0.1:8888");
//				subMap.put("domain","http://127.0.0.1:8888");
//				appinfoMap.put(1001,subMap);
//				setValue("/masterRoot/appInfoService_apps", JsonUtils.format(appinfoMap), null);

//				root="masterRoot";
//				projectName="master";
//				List<String> secretService_keys =new ArrayList<>();
//				secretService_keys.add("syswin#testKey");
//				secretService_keys.add("syswin#testKey");
//				setValue("/masterRoot/secretService_keys", JsonUtils.format(secretService_keys), null);
		
		

		Thread.sleep(1000);

	}


	public static void main11(String[] sdf) throws Exception{
		//listen();
		zkAddress="172.28.5.132:3181,172.28.5.133:3181,172.28.5.134:3181";

		
		
		

		//set value for bufferCache
		projectName="bufferCache";
		Map<String,List<String>> groupIps=new HashMap<>();
		List<String> ips1=new ArrayList<>();
		List<String> ips2=new ArrayList<>();
		List<String> ips3=new ArrayList<>();
		ips1.add("172.28.5.131:33001");
		ips2.add("172.28.5.132:33001");
		ips1.add("172.28.5.133:33001");
		ips3.add("172.28.5.134:33001");
		groupIps.put("bufferCacheGroup1",ips1);
		groupIps.put("bufferCacheGroup2",ips2);
		groupIps.put("bufferCacheGroup3",ips3);
		setValue("/bufferCacheRoot/groupIps", JsonUtils.format(groupIps), null,zkAddress,"bufferCache");
		setValue("/bufferCacheRoot/oldGroupIps",JsonUtils.format(groupIps),null,zkAddress,"bufferCache");


				//set value for chunk
				projectName="chunk";
				Map<String,List<String>> chunkGroupIps=new HashMap<>();
				List<String> chunkIps1=new ArrayList<>();
				List<String> chunkIps2=new ArrayList<>();
				chunkIps1.add("172.28.5.131:32001");
				chunkIps1.add("172.28.5.132:32001");
				chunkIps2.add("172.28.5.133:32001");
				chunkIps2.add("172.28.5.134:32001");
				chunkGroupIps.put("chunkGroup1",chunkIps1);
				chunkGroupIps.put("chunkGroup2",chunkIps2);
				setValue("/chunkRoot/groupIps", JsonUtils.format(chunkGroupIps), null,zkAddress,"chunk");
				setValue("/chunkRoot/oldGroupIps",JsonUtils.format(chunkGroupIps),null,zkAddress,"chunk");


				//set value for computing 	computingNodes
				//@ConfigProperty(path = "computingNodes", json = true)
				//public  Map<String,Integer> computingNodes =new HashMap<>();
				projectName="computing";
				Map<String,Integer> computingNodes =new HashMap<>();
				computingNodes.put("172.28.5.131:34001",10);
				computingNodes.put("172.28.5.132:34001",10);
				computingNodes.put("172.28.5.133:34001",1);
				computingNodes.put("172.28.5.134:34001",1);
				setValue("/computingRoot/computingNodes", JsonUtils.format(computingNodes), null,zkAddress,"computing");



				//set value for master
				projectName="master";
				setValue("/masterRoot/master_cleanStartTime","2",null,zkAddress,"master");
				setValue("/masterRoot/master_cleanEndTime","8",null,zkAddress,"master");



				//set value for master dispatchCenter_app2idc
				projectName="master";
				Map<Integer, Map<Object, Integer>> app2idc=new HashMap<>();
				Map<Object, Integer> map= new HashMap<Object, Integer>();
				map.put("1",10);
				map.put("2",0);
				app2idc.put(1001,map);
				//APPID为1001的 对应两个机房1，和2. 其中1的权重是20% 2的权重是80%。
				setValue("/masterRoot/dispatchCenter_app2idc", JsonUtils.format(app2idc), null,zkAddress,"master");

				//set value for master dispatchCenter_fstype2domain
				//@ConfigProperty(path = "dispatchCenter_fstype2domain", json = true,typeReference=MapIntegerStringTypeMode.class)
				//public Map<Integer,String> fstype2domain;
				projectName="master";
				Map<Integer,String> fstype2domain=new HashMap<>();
				fstype2domain.put(1,"http://d1.scloud.systoon.com");
				
//				fstype2domain.put(1,"http://172.28.6.136");
//				fstype2domain.put(2,"http://172.28.6.136");
				//fstype为1的IP是http://127.0.0.1:8880
				//fstype为2的IP是http://127.0.0.1:8881
				setValue("/masterRoot/dispatchCenter_fstype2domain", JsonUtils.format(fstype2domain), null,zkAddress,"master");


				//set value for master dispatchCenter_idc2domain
				//@ConfigProperty(path = "dispatchCenter_idc2domain", json = true,typeReference=MapIntegerStringTypeMode.class)
				//Map<Integer,String> idc2domain;
				projectName="master";
				Map<Integer,String> idc2domain=new HashMap<>();

				idc2domain.put(1,"http://d1.scloud.systoon.com");
				
//				idc为1的IP是http://127.0.0.1:8880
//				idc为2的IP是http://127.0.0.1:8881
				setValue("/masterRoot/dispatchCenter_idc2domain", JsonUtils.format(idc2domain), null,zkAddress,"master");





				//set value for master 	appInfoService_apps
				// Map<Integer, AppInfo> appinfoMap;
				projectName="master";
				Map<Integer, Map> appinfoMap=new HashMap<>();
				Map<String, Object> subMap= new HashMap<String, Object>();
				subMap.put("appid",1001);
				subMap.put("accessKeyId","sdfeggs");
				subMap.put("accessKeySecret","gregergher");
				subMap.put("callbakcUrl","http://127.0.0.1:8888");
				subMap.put("domain","http://127.0.0.1:8888");
				appinfoMap.put(1001,subMap);
				setValue("/masterRoot/appInfoService_apps", JsonUtils.format(appinfoMap), null,zkAddress,"master");

				projectName="master";
				List<String> secretService_keys =new ArrayList<>();
				secretService_keys.add("syswin#onlineSecretServiceKey");
				secretService_keys.add("syswin#onlineSecretServiceKey");
				setValue("/masterRoot/secretService_keys", JsonUtils.format(secretService_keys), null,zkAddress,"master");
		
		

		Thread.sleep(1000);
		
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
	
	
	public static void setValue(String path,String value,String ip,String zkAddress,String projectName) throws Exception{
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
		client.setData().forPath(path,JsonUtils.objectMapper.writeValueAsBytes(zkResult) );
	}
	
	
	
}
