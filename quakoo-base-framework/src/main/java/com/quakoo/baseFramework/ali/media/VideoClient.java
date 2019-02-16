package com.quakoo.baseFramework.ali.media;

import com.google.common.collect.Maps;
import com.quakoo.baseFramework.http.HttpPoolParam;
import com.quakoo.baseFramework.http.HttpResult;
import com.quakoo.baseFramework.http.MultiHttpPool;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


public class VideoClient {

    private String accessKey;
    private String accessSecret;
    private String liveDomainName;

    private static final String version = "2017-03-21";
    private static final String signMethod = "hmac-sha1";
    private static final String signVersion = "1.0";
    private static final String gateway = "http://vod.cn-shanghai.aliyuncs.com";

    public VideoClient(String accessKey, String accessSecret, String liveDomainName) {
    	this.accessKey = accessKey;
    	this.accessSecret = accessSecret;
    	this.liveDomainName = liveDomainName;
    }



    public List<LiveRecordVideo> listLiveRecordVideo(String appName, String streamName) throws Exception {
    	if(StringUtils.isBlank(appName)) throw new IllegalArgumentException("appName is null!");
    	if(StringUtils.isBlank(streamName)) throw new IllegalArgumentException("streamName is null!");
    	String action = "ListLiveRecordVideo";
    	Map<String, String> params = Maps.newHashMap();
    	params.put("AppName", appName);
    	params.put("StreamName", streamName);
		params.put("DomainName", liveDomainName);
		params.put("PageSize", "100");
    	Result xingYuResult = this.send(action, params);

    	if(xingYuResult.getLiveRecordVideoList()!=null&&xingYuResult.getLiveRecordVideoList()!=null
    			&&xingYuResult.getLiveRecordVideoList().getLiveRecordVideo().size()>0){
    		for(LiveRecordVideo liveRecordVideo:xingYuResult.getLiveRecordVideoList().getLiveRecordVideo()){
				Video video=liveRecordVideo.getVideo();
				video.setPlayInfos(getPlayInfo(video.getVideoId()));
			}
		}
    	if(xingYuResult.getLiveRecordVideoList()==null){
    		return new ArrayList<>();
    	}
    	return xingYuResult.getLiveRecordVideoList().getLiveRecordVideo();
    }


	public void deleteVideo(String videoId) throws Exception {
		if(StringUtils.isBlank(videoId)) throw new IllegalArgumentException("appName is null!");
		String action = "DeleteVideo";
		Map<String, String> params = Maps.newHashMap();
		params.put("VideoIds", videoId);
		this.send(action, params);
	}


	public List<PlayInfo> getPlayInfo(String videoId) throws Exception {
		String action = "GetPlayInfo";
		Map<String, String> params = Maps.newHashMap();
		params.put("VideoId", videoId);
		Result xingYuResult = this.send(action, params);
		if(xingYuResult.getPlayInfoList()==null){return new ArrayList<>();}
		return xingYuResult.getPlayInfoList().getPlayInfo();
	}

	public Mezzanine getMezzanine(String videoId, long second) throws Exception {
        String action = "GetMezzanineInfo";
        Map<String, String> params = Maps.newHashMap();
        params.put("VideoId", videoId);
        params.put("AuthTimeout", String.valueOf(second));
        Result result = this.send(action, params);
        return result.getMezzanine();
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
	    //queryString = createLinkEncodeString(allParams);
	    //url = url + "?" + queryString;
	    //HttpParam httpParam = new HttpParam(5000, 5000, 1);
	    HttpPoolParam httpParam=new HttpPoolParam(10000, 10000, 1);
		MultiHttpPool pool=MultiHttpPool.getMultiHttpPool(httpParam);

	    HttpResult httpResult = pool.httpQuery(url, allParams, "GET", true,true);
	    Result xingYuResult = JsonUtils.fromJson(httpResult.getResult(), Result.class);
	    return xingYuResult;
	}

	private String createLinkEncodeString(Map<String, String> params) throws Exception {
		StringBuilder stringBuilder = new StringBuilder();
		for (Entry<String, String> entry : params.entrySet()) {
			String key = encode(entry.getKey(), "UTF-8");
			String value = encode(entry.getValue(), "UTF-8").replace("+", "%2B").replace("=", "%3D");
			stringBuilder.append(key).append("=").append(value).append("&");
		}
		return stringBuilder.substring(0, stringBuilder.length() - 1);
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



	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//b0d4261ccc8e484288dee1f44d37fc8a.mp4
//		VideoClient liveClient=new VideoClient("j7aHxtaMnUS4aZJG", "4mghcK0qx4zn5oDdNKUvRfyniCcDA2", "live2.quakoo.com");	
//		List<LiveRecordVideo>  livs =liveClient.listLiveRecordVideo("main", "1");
//		System.out.println(livs);
		VideoClient liveClient=new VideoClient("j7aHxtaMnUS4aZJG", "4mghcK0qx4zn5oDdNKUvRfyniCcDA2", null);
		Mezzanine mezzanine = liveClient.getMezzanine("bb47b81858c5486b93c99f3bf77bbe14", 60 * 60);
		System.out.println(JsonUtils.toJson(mezzanine));

	}
	
}
