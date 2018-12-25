package com.quakoo.baseFramework.sms;

import java.util.Arrays;
import java.util.Random;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.sms.SmsClient;
import com.baidubce.services.sms.SmsClientConfiguration;
import com.baidubce.services.sms.model.SendMessageRequest;

public class SmsUtil {
	
	private static final String access_key_id = "48034906d1f447daa2ae2f8e94067727";
	private static final String secret_access_key = "5be25b3c5bca47eba4bd8052fec7a01c";
	private static final SmsClientConfiguration config;
	static{
		config = new SmsClientConfiguration();
	    config.setCredentials(new DefaultBceCredentials(access_key_id,secret_access_key));
	}
	private static final String bang_sms_id = "smsTpl:d7d659b40dbe401195e0501f9873a38d";
	private static final String donger_sms_id = "smsTpl:22e4fbe5-45c2-4b20-b720-70208dcbf0ca";
	
	public static boolean send_sms_bang(String phone,String code,String minute){
	    SmsClient client = new SmsClient(config);
	    
	    SendMessageRequest request = new SendMessageRequest();// 构建SendMessageRequest对象
	    request.setTemplateId(bang_sms_id);// 设置短信模板ID
	    request.setContentVar("{\"code\" : \""+code+"\" , \"hour\" : \""+minute+"\"}");// 设置短信模板内容变量的替换值
	    request.setReceiver(Arrays.asList(new String[]{phone}));// 设置接收人列表
	    try {
	    	 client.sendMessage(request);// 请求Server
	    	 return true;
		} catch (Exception e) {
			 return false;
		}
	}
	
	public static boolean send_sms_donger(String phone,String code,String minute){
	    SmsClient client = new SmsClient(config);
	    
	    SendMessageRequest request = new SendMessageRequest();// 构建SendMessageRequest对象
	    request.setTemplateId(donger_sms_id);// 设置短信模板ID
	    request.setContentVar("{\"code\" : \""+code+"\" , \"hour\" : \""+minute+"\"}");// 设置短信模板内容变量的替换值
	    request.setReceiver(Arrays.asList(new String[]{phone}));// 设置接收人列表
	    try {
	    	 client.sendMessage(request);// 请求Server
	    	 return true;
		} catch (Exception e) {
			 return false;
		}
	}

	/**
	 * @param args
	 * smsTpl:e7476122a1c24e37b3b0de19d04ae902
	 * smsTpl:d7d659b40dbe401195e0501f9873a38d
	 */
	public static void main(String[] args) {
	System.out.println(send_sms_donger("15011226135","8888","30"));
		
		Random random = new Random();
		System.out.println(random.nextInt(9999));
	}

}
