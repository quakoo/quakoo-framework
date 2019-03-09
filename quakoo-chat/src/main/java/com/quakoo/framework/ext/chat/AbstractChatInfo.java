package com.quakoo.framework.ext.chat;

import java.util.List;

import com.quakoo.baseFramework.redis.JedisBean;
//import com.quakoo.framework.ext.chat.bean.SegmentLock;
import com.quakoo.framework.ext.chat.util.PropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Lists;
import redis.clients.jedis.JedisPoolConfig;

public abstract class AbstractChatInfo implements InitializingBean {
	
	private PropertyUtil propertyUtil = PropertyUtil.getInstance("chat.properties");
	
	public static final int redis_overtime_long = 60 * 60 * 24 * 3;
	
	public static final int redis_overtime_short = 5;

	public static final String lock_suffix = "_lock";
	
	public static final int lock_timeout = 60000;
	
	public static final int session_timout = 5000;
	
	public static final int pull_length = 500;

	public List<String> single_chat_queue_names = Lists.newArrayList();
    public List<String> many_chat_queue_names = Lists.newArrayList();

    public List<String> user_stream_queue_names = Lists.newArrayList();
    public List<String> user_info_queue_names = Lists.newArrayList();

	public List<String> user_info_table_names = Lists.newArrayList();
//	public List<String> single_chat_queue_table_names = Lists.newArrayList();
//	public List<String> many_chat_queue_table_names = Lists.newArrayList();
	public List<String> message_table_names = Lists.newArrayList();
	public List<String> chat_group_table_names = Lists.newArrayList();
	public List<String> user_directory_table_names = Lists.newArrayList();
	public List<String> user_stream_table_names = Lists.newArrayList();
	public List<String> user_client_info_table_names = Lists.newArrayList();
	public List<String> user_prompt_table_names = Lists.newArrayList();
	
	public String projectName; //项目名
	public String lockZkAddress; //分布式锁
//	public String lockProjectName;
	public String nioConnectBootstrapIp; //长连接IP
	public String nioConnectBootstrapPort; //长连接端口
	public String distributedZkAddress; //分布式配置地址
//	public String distributedProjectName;
	
	public String redis_will_push_queue;
	public String user_stream_init_lock_key; //信息流锁key


//    public SegmentLock segmentLock; //分段锁 用于mysql限制insert的峰值(同一ms只能有3个insert操作)

    public JedisPoolConfig queueConfig;
    public JedisBean queueInfo;
	
	protected void init(int tableNum) {
		this.projectName = propertyUtil.getProperty("chat.project.name");
		this.lockZkAddress = propertyUtil.getProperty("chat.lock.zk.address");
//		this.lockProjectName = propertyUtil.getProperty("chat.lock.project.name");
		this.nioConnectBootstrapIp = propertyUtil.getProperty("chat.nio.connect.bootstrap.ip");
		this.nioConnectBootstrapPort = propertyUtil.getProperty("chat.nio.connect.bootstrap.port");
		this.distributedZkAddress = propertyUtil.getProperty("chat.distributed.zk.address");
//		this.distributedProjectName = propertyUtil.getProperty("chat.distributed.project.name");

        String single_chat_queue_name = "single_chat_queue";
        String many_chat_queue_name = "many_chat_queue";

        String user_stream_queue_name = "user_stream_queue";
        String user_info_queue_name = "user_info_queue";

		String user_info_table_name = "user_info";
//		String single_chat_queue_table_name = "single_chat_queue";
//		String many_chat_queue_table_name = "many_chat_queue";
		String message_table_name = "message";
		String chat_group_table_name = "chat_group";
		String user_directory_table_name = "user_directory";
		String user_stream_table_name = "user_stream";
		String user_client_info_table_name = "user_client_info";
		String user_prompt_table_name = "user_prompt";
		for(int i = 0; i < tableNum; i++) {
			if(i == 0) {
                single_chat_queue_names.add(single_chat_queue_name);
                many_chat_queue_names.add(many_chat_queue_name);

                user_stream_queue_names.add(user_stream_queue_name);
                user_info_queue_names.add(user_info_queue_name);

				user_info_table_names.add(user_info_table_name);
//				single_chat_queue_table_names.add(single_chat_queue_table_name);
//				many_chat_queue_table_names.add(many_chat_queue_table_name);
				message_table_names.add(message_table_name);
				chat_group_table_names.add(chat_group_table_name);
				user_directory_table_names.add(user_directory_table_name);
				user_stream_table_names.add(user_stream_table_name);
				user_client_info_table_names.add(user_client_info_table_name);
				user_prompt_table_names.add(user_prompt_table_name);
			} else {
                single_chat_queue_names.add(single_chat_queue_name + "_" + i);
                many_chat_queue_names.add(many_chat_queue_name + "_" + i);

                user_stream_queue_names.add(user_stream_queue_name + "_" + i);
                user_info_queue_names.add(user_info_queue_name + "_" + i);

				user_info_table_names.add(user_info_table_name + "_" + i);
//				single_chat_queue_table_names.add(single_chat_queue_table_name + "_" + i);
//				many_chat_queue_table_names.add(many_chat_queue_table_name + "_" + i);
				message_table_names.add(message_table_name + "_" + i);
				chat_group_table_names.add(chat_group_table_name + "_" + i);
				user_directory_table_names.add(user_directory_table_name + "_" + i);
				user_stream_table_names.add(user_stream_table_name + "_" + i);
				user_client_info_table_names.add(user_client_info_table_name + "_" + i);
				user_prompt_table_names.add(user_prompt_table_name + "_" + i);
			}
		}
		redis_will_push_queue = projectName + "_will_push_queue";
		user_stream_init_lock_key = projectName + "_user_stream_init_uid_%d_lock";

//        segmentLock = new SegmentLock(3, false); //3个分段锁

        String queueRedisAddress = propertyUtil.getProperty("chat.queue.redis.address");
        String queueRedisPassword = propertyUtil.getProperty("chat.queue.redis.password");
        if (StringUtils.isBlank(queueRedisAddress) || StringUtils.isBlank(queueRedisPassword)) {
            throw new IllegalStateException("队列配置不能为空");
        }

        queueConfig = new JedisPoolConfig();
        queueConfig.setMaxTotal(50);
        queueConfig.setMaxIdle(25);
        queueConfig.setMinIdle(10);
        queueConfig.setMaxWaitMillis(1000);
        queueConfig.setTestOnBorrow(true);
        queueConfig.setTestWhileIdle(true);

        queueInfo = new JedisBean();
        queueInfo.setMasterAddress(queueRedisAddress);
        queueInfo.setPassword(queueRedisPassword);
    }
	
}
