package com.quakoo.baseFramework.ali.media;

import com.google.common.collect.Maps;
import com.quakoo.baseFramework.http.HttpPoolParam;
import com.quakoo.baseFramework.http.HttpResult;
import com.quakoo.baseFramework.http.MultiHttpPool;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import com.quakoo.baseFramework.secure.MD5Utils;
import com.quakoo.live.room.model.LiveRoom;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;


public class LiveClient {

    private String accessKey;
    private String accessSecret;
    private String liveDomainName;

    private static final String version = "2016-11-01";
    private static final String signMethod = "hmac-sha1";
    private static final String signVersion = "1.0";
    private static final String gateway = "http://live.aliyuncs.com";
    
    public LiveClient(String accessKey, String accessSecret,String liveDomainName) {
    	this.accessKey = accessKey;
    	this.accessSecret = accessSecret;
    	this.liveDomainName = liveDomainName;
    }


    
    public Result resumeLiveStream(String appName, String streamName) throws Exception {
    	if(StringUtils.isBlank(appName)) throw new IllegalArgumentException("appName is null!");
    	if(StringUtils.isBlank(streamName)) throw new IllegalArgumentException("streamName is null!");
    	String action = "ResumeLiveStream";
    	Map<String, String> params = Maps.newHashMap();
    	params.put("AppName", appName);
    	params.put("StreamName", streamName);
		params.put("LiveStreamType", "publisher");
		params.put("DomainName", liveDomainName);
    	Result result = this.send(action, params);
    	return result;
    }


	public Result denyLiveStream(String appName, String streamName) throws Exception {
		if(StringUtils.isBlank(appName)) throw new IllegalArgumentException("appName is null!");
		if(StringUtils.isBlank(streamName)) throw new IllegalArgumentException("streamName is null!");
		String action = "ForbidLiveStream";
		Map<String, String> params = Maps.newHashMap();
		params.put("AppName", appName);
		params.put("StreamName", streamName);
		params.put("LiveStreamType", "publisher");
		params.put("DomainName", liveDomainName);
		Result result = this.send(action, params);
		return result;
	}

    

    
	private Result send(String action, Map<String, String> params) throws Exception {
		Map<String, Object> allParams = Maps.newTreeMap();
		if(null != params) allParams.putAll(params);
		Date date = new Date();
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    df.setTimeZone(TimeZone.getTimeZone("GMT+:08:00"));
	    allParams.put("Format","JSON");
		allParams.put("Version", version);
		allParams.put("AccessKeyId", accessKey);
		allParams.put("SignatureMethod", signMethod);
		allParams.put("Timestamp", df.format(date));
	    allParams.put("SignatureVersion", signVersion);
	    allParams.put("SignatureNonce", UUID.randomUUID().toString());
	    allParams.put("Action", action);
	    StringBuilder stringBuilder = new StringBuilder();
		for (String key : allParams.keySet()) {
			stringBuilder.append(encode(key, "UTF-8"));
			stringBuilder.append("=");
			stringBuilder.append(encode(allParams.get(key).toString(), "UTF-8"));
			stringBuilder.append("&");
		}
		String queryString = stringBuilder.substring(0, stringBuilder.length() - 1);
	    queryString = "GET&%2F&" + encode(queryString, "UTF-8");
    	String key = accessSecret + "&";
    	byte[] signData = hmacSha1(queryString.getBytes("UTF-8"), key.getBytes("UTF-8"));
    	allParams.put("Signature", Base64Util.encode(signData));
	    String url = gateway ;

	    HttpPoolParam httpParam=new HttpPoolParam(10000, 10000, 1);
		MultiHttpPool pool=MultiHttpPool.getMultiHttpPool(httpParam);
	    
	    HttpResult httpResult = pool.httpQuery(url, allParams, "GET", true,true);
	    
	    Result result = JsonUtils.fromJson(httpResult.getResult(), Result.class);
	    return result;
	}
	
    private byte[] hmacSha1(byte[] data, byte[] key) throws InvalidKeyException, NoSuchAlgorithmException {
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data);
        return rawHmac;
    }
    
    private String encode(String str, String enc) throws Exception {
    	String result = URLEncoder.encode(str, enc);
        return result.replace("%21", "!").replace("%40", "@").replace("%24", "$").replace("%7E", "~").replace("%2C", ",").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("+", "%20");
    }

    public String getPushRtmp(long id, long startTime, String pushServerDomain, String domain, String appName, String authKey) throws Exception {
        String pushRtmpFormat = "rtmp://%s/%s/%d?vhost=%s&auth_key=%d-0-0-%s";
        startTime = startTime/1000;
        String url=String.format("/%s/%d-%d-0-0-%s", appName, id, startTime, authKey);
        String authValue= MD5Utils.md5ReStr(url.getBytes());
        String pushRtmp = String.format(pushRtmpFormat,pushServerDomain, appName, id, domain, startTime, authValue);
        return pushRtmp;
    }

    public String getPlayUrl(long id, int playType, String appName, String authKey, String domain) throws Exception {
        long time=System.currentTimeMillis()/1000;
        String playUrlFormat = "rtmp://%s/%s/%d?auth_key=%d-0-0-%s";
        String url=String.format("/%s/%d-%d-0-0-%s", appName, id, time, authKey);
        if(playType== LiveRoom.playType_m3u8){
            playUrlFormat =  "http://%s/%s/%d.m3u8?auth_key=%d-0-0-%s";
            url=String.format("/%s/%d.m3u8-%d-0-0-%s", appName, id, time, authKey);
        }
        if(playType==LiveRoom.playType_flv){
            playUrlFormat =   "http://%s/%s/%d.flv?auth_key=%d-0-0-%s";
            url=String.format("/%s/%d.flv-%d-0-0-%s", appName, id, time, authKey);
        }
        String authValue= MD5Utils.md5ReStr(url.getBytes());
        String playUrl = String.format(playUrlFormat, domain, appName, id, time, authValue);
        return playUrl;
    }

    public String getCover(long id, String coverDomain, String appName) {
        String coverUrlFormat = "http://%s/%s/%d.jpg";
        String res = String.format(coverUrlFormat, coverDomain, appName, id);
        return res;
    }

}
