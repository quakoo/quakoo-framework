package com.quakoo.baseFramework.apicloud;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.quakoo.baseFramework.http.MultiHttpPool;
import org.apache.commons.lang.StringUtils;

import com.quakoo.baseFramework.http.MultiHttpPool;
import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.secure.Sha1Utils;

public class PushUtil {
	public static void push(Collection<String> uids, PushMsg pushMsg, long time,
			String appId, String appKey) throws Exception {

		String uidsString = "";
		for (String uid : uids) {
			uidsString = uidsString + uid + ",";
		}
		if (StringUtils.isBlank(uidsString)) {
			return;
		}
		uidsString = uidsString.substring(0, uidsString.length() - 1);

		String varappKey = Sha1Utils
				.sha1ReStr((appId + "UZ" + appKey + "UZ" + time).getBytes())
				+ "." + time;

		//
		// title–消息标题，
		// content – 消息内容
		// type – 消息类型，1:消息 2:通知
		// platform - 0:全部平台，1：ios, 2：android
		// groupName - 推送组名，多个组用英文逗号隔开.默认:全部组。eg.group1,group2 .
		// userIds - 推送用户id, 多个用户用英文逗号分隔，eg. user1,user2。
		MultiHttpPool httpPool = MultiHttpPool.getMultiHttpPool(20000);
		Map<String, String> headMap = new HashMap<>();
		headMap.put("X-APICloud-AppId", appId);
		headMap.put("X-APICloud-AppKey", varappKey);

		Map<String, Object> postFromParamsMap = new HashMap<>();
		
		postFromParamsMap.put("title", "1-6岁孩子的心理发展特征，你知道多少呢？");
//		postFromParamsMap.put("content", JsonUtils.format(pushMsg));
		postFromParamsMap.put("content", "1-6岁孩子的心理发展特征，你知道多少呢？");
		postFromParamsMap.put("type", "2");
		postFromParamsMap.put("platform", "1");
		postFromParamsMap.put("userIds", uidsString);

		httpPool.httpQueryWithRety("https://p.apicloud.com/api/push/message",
				null, "post", headMap, postFromParamsMap, null, false, true,
				true);
	}

	public static void main(String[] fwe) throws Exception {
		String message="测试下";
		
		push(Arrays.asList(new String[] { "" }), new PushMsg(1, message), new Date().getTime(),
				"A6981303514490", "3D2181E6-B928-F8C7-031D-58384579C358");
	}
}
