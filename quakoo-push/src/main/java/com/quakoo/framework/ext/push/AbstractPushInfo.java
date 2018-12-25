package com.quakoo.framework.ext.push;

import java.util.List;

import com.quakoo.framework.ext.push.util.PropertyUtil;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Lists;

public abstract class AbstractPushInfo implements InitializingBean {

	private PropertyUtil propertyUtil = PropertyUtil.getInstance("push.properties");
	
	public List<String> payload_table_names = Lists.newArrayList();
	public List<String> push_handle_queue_table_names = Lists.newArrayList();
	public List<String> push_user_info_pool_table_names = Lists.newArrayList();
	public List<String> push_user_queue_table_names = Lists.newArrayList();

    public String projectName;
	public String iosPushCertificateFileName;
	public String iosPushPassword;
	public String pushLockZkAddress;
	public String pushNioConnectIp;
	public String pushNioConnectPort;
	public String distributedZkAddress;

	public String androidXiaomiPushSecretkey;
	public String androidXiaomiPushPackagename;

    public String androidHuaweiPushAppsecret;
    public String androidHuaweiPushAppid;
    public String androidHuaweiPushPackagename;
    public String androidHuaweiPushActivity;
    public String androidHuaweiPushScheme;

    public String androidMeizuPushAppsecret;
    public String androidMeizuPushAppid;
	
	public String lock_suffix = "_lock";
    public int redis_overtime_long = 60 * 60 * 24 * 5;
	public int redis_overtime_short = 5;
    public int lock_timeout = 60000;
	public int session_timout = 5000;
	
	protected void init(int tableNum) {
	    this.projectName = propertyUtil.getProperty("project.name");
		this.iosPushCertificateFileName = propertyUtil.getProperty("ios.push.certificate.file.name");
		this.iosPushPassword = propertyUtil.getProperty("ios.push.password");
		this.pushLockZkAddress = propertyUtil.getProperty("push.lock.zk.address");
		this.pushNioConnectIp = propertyUtil.getProperty("push.nio.connect.bootstrap.ip");
		this.pushNioConnectPort = propertyUtil.getProperty("push.nio.connect.bootstrap.port");
		this.distributedZkAddress = propertyUtil.getProperty("push.distributed.zk.address");
		this.androidXiaomiPushSecretkey = propertyUtil.getProperty("android.xiaomi.push.secretkey");
        this.androidXiaomiPushPackagename = propertyUtil.getProperty("android.xiaomi.push.packagename");
        this.androidHuaweiPushAppid = propertyUtil.getProperty("android.huawei.push.appid");
        this.androidHuaweiPushAppsecret = propertyUtil.getProperty("android.huawei.push.appsecret");
        this.androidHuaweiPushPackagename = propertyUtil.getProperty("android.huawei.push.packagename");
        this.androidHuaweiPushActivity = propertyUtil.getProperty("android.huawei.push.activity");
        this.androidHuaweiPushScheme = propertyUtil.getProperty("android.huawei.push.scheme");
        this.androidMeizuPushAppsecret = propertyUtil.getProperty("android.meizu.push.appsecret");
        this.androidMeizuPushAppid = propertyUtil.getProperty("android.meizu.push.appid");

		String payload_table_name = "payload";
		String push_handle_queue_table_name = "push_handle_queue";
		String push_user_info_pool_table_name = "push_user_info_pool";
		String push_user_queue_table_name = "push_user_queue";
		for(int i = 0; i < tableNum; i++) {
			if(i == 0) {
				push_handle_queue_table_names.add(push_handle_queue_table_name);
				payload_table_names.add(payload_table_name);
				push_user_info_pool_table_names.add(push_user_info_pool_table_name);
				push_user_queue_table_names.add(push_user_queue_table_name);
			} else {
				push_handle_queue_table_names.add(push_handle_queue_table_name + "_" + i);
				payload_table_names.add(payload_table_name + "_" + i);
				push_user_info_pool_table_names.add(push_user_info_pool_table_name + "_" + i);
				push_user_queue_table_names.add(push_user_queue_table_name + "_" + i);
			}
		}
	}
	
}
