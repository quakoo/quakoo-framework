package test;

import java.util.List;
import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

public class Test {
	
	private static String chatHelpPath = "/chat_help";
	
	private static void print(List<ChildData> childData) throws Exception {
		if (childData != null) {  
		    for (ChildData data : childData) {  
		    	String path = data.getPath();
		    	System.out.println(path.substring(path.lastIndexOf("/")+1));
		    }  
		} 
	}

	public static void main(String[] args) throws Exception {
		String serialNumber = UUID.randomUUID().toString();
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory
				.builder();
		CuratorFramework client = builder
				.connectString("182.92.191.75:2181")
				.sessionTimeoutMs(20000)
				.connectionTimeoutMs(5000)
				.canBeReadOnly(false)
				.retryPolicy(
						new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
				.namespace("fangpai").defaultData(null).build();
		client.start();
		Stat stat=client.checkExists().forPath(chatHelpPath);
		if(null == stat){
			client.create().creatingParentsIfNeeded().
					withMode(CreateMode.PERSISTENT).forPath(chatHelpPath);
		} else {
			String path = chatHelpPath + "/" + serialNumber;
			client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
			
			final PathChildrenCache cached = new PathChildrenCache(client, chatHelpPath, true);
			cached.getListenable().addListener(new PathChildrenCacheListener() {
				@Override
				public void childEvent(CuratorFramework client,
						PathChildrenCacheEvent event) throws Exception {
					PathChildrenCacheEvent.Type eventType = event.getType();
					switch (eventType) {
					case CHILD_ADDED: {
						System.out.println("---添加---");
						print(cached.getCurrentData());
						break;
					}
					case CHILD_UPDATED: {
						System.out.println("---更新---");
						print(cached.getCurrentData());
						break;
					}
					case CHILD_REMOVED: {
						System.out.println("---删除---");
						print(cached.getCurrentData());
						break;
					}
					default:
						break;
					}
				}
			});
			cached.start(StartMode.BUILD_INITIAL_CACHE);
			print(cached.getCurrentData());
			Thread.sleep(Long.MAX_VALUE);
		}
	}
}
